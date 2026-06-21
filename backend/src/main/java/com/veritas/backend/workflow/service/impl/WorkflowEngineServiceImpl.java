package com.veritas.backend.workflow.service.impl;

import com.veritas.backend.audit.entity.AuditLog;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.audit.service.impl.AuditServiceImpl;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.common.exception.WorkflowStateException;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestStatus;
import com.veritas.backend.requisition.entity.ClosedReason;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.repository.QuoteRepository;
import com.veritas.backend.workflow.entity.TransitionRule;
import com.veritas.backend.integrations.currency.service.CurrencyConversionService;
import com.veritas.backend.integrations.jira.service.JiraSyncService;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.entity.WorkflowTransition;

import com.veritas.backend.workflow.validation.WorkflowBranchingContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.veritas.backend.workflow.repository.TransitionRuleRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.workflow.repository.WorkflowTransitionRepository;
import com.veritas.backend.workflow.service.WorkflowEngineService;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.veritas.backend.common.model.AuditActionConstants.APPROVE;
import static com.veritas.backend.common.model.AuditActionConstants.REVERT;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngineServiceImpl implements WorkflowEngineService {

    /**
     * Maximum number of consecutive gateway hops the engine will traverse
     * in a single moveToNextStep call chain. Prevents StackOverflowError
     * if a gateway-only cycle somehow passes validation.
     */
    private static final int MAX_GATEWAY_RECURSION_DEPTH = 50;

    private final WorkflowStepRepository workflowStepRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final TransitionRuleRepository transitionRuleRepository;
    private final QuoteRepository quoteRepository;
    private final AuditServiceImpl auditService;
    private final AuditLogRepository auditLogRepository;
    private final CurrencyConversionService currencyConversionService;

    @Lazy
    private final JiraSyncService jiraSyncService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void moveToNextStep(Request request, User actor, Long nextAssigneeId) {
        moveToNextStep(request, actor, 0, nextAssigneeId);

        if (jiraSyncService != null) {
            jiraSyncService.handleVeritasWorkflowChange(request);
        }
    }

    private void moveToNextStep(Request request, User actor, int gatewayDepth, Long nextAssigneeId) {
        if (gatewayDepth > MAX_GATEWAY_RECURSION_DEPTH) {
            throw new WorkflowStateException(
                    "Workflow execution exceeded maximum gateway traversal depth (" + MAX_GATEWAY_RECURSION_DEPTH
                            + "). This likely indicates a cycle in the workflow definition.");
        }

        WorkflowStep currentStep = request.getCurrentStep();

        checkAuthorization(request, actor, currentStep);

        List<WorkflowTransition> transitions = getPrioritizedTransitions(currentStep);

        for (WorkflowTransition transition : transitions) {
            if (checkCondition(request, transition)) {
                assertBudgetWithinSafetyBuffer(request.getBudget());

                WorkflowStep toStep = transition.getToStep();
                request.setCurrentStep(toStep);

                if (toStep.getWorkflowComponent() != WorkflowComponent.END_EVENT &&
                        toStep.getWorkflowComponent() != WorkflowComponent.BRANCH) {
                    if (toStep.getRole() == UserRole.REQUESTER) {
                        if (Boolean.TRUE.equals(toStep.getIsTeamLeader())) {
                            if (request.getUser().getTeam() == null || request.getUser().getTeam().getLeader() == null) {
                                throw new WorkflowStateException("The requester's team leader could not be resolved because the requester does not belong to a team or the team has no team leader assigned.");
                            }
                            request.setAssignee(request.getUser().getTeam().getLeader());
                        } else {
                            request.setAssignee(request.getUser());
                        }
                    } else if (nextAssigneeId != null) {
                        User nextAssignee = userRepository.findById(nextAssigneeId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Assignee not found with ID: " + nextAssigneeId));
                        if (toStep.getRole() != null && !nextAssignee.getRole().equals(toStep.getRole())) {
                            throw new IllegalArgumentException("User " + nextAssignee.getName()
                                    + " does not have the required role: " + toStep.getRole());
                        }
                        request.setAssignee(nextAssignee);
                    } else {
                        request.setAssignee(null);
                    }
                } else {
                    request.setAssignee(null);
                }

                Optional<TransitionRule> optRule = transitionRuleRepository.findByTransition(transition);
                if (optRule.isPresent()) {
                    TransitionRule rule = optRule.get();

                    List<String> validationErrors = new ArrayList<>();
                    List<String> missingAttachments = new ArrayList<>();

                    if (rule.getMinRequiredVendors() != null && rule.getMinRequiredVendors() > 0) {
                        Long requestId = request.getRequestID();
                        List<Quote> quotes = requestId != null
                                ? quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(requestId)
                                : List.of();
                        long distinctVendors = quotes.stream()
                                .map(Quote::getVendorID)
                                .filter(Objects::nonNull)
                                .map(Vendor::getId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .count();
                        if (distinctVendors < rule.getMinRequiredVendors()) {
                            validationErrors.add("A minimum of " + rule.getMinRequiredVendors() + " distinct vendors is required");
                        }
                    }

                    if (rule.getMinVendorReliabilityScore() != null) {
                        Quote selectedQuote = request.getSelectedQuote();
                        if (selectedQuote != null) {
                            Vendor vendor = selectedQuote.getVendorID();
                            if (vendor != null) {
                                Double score = vendor.getOverallScore();
                                double actualScore = (score != null) ? score : 0.0;
                                if (actualScore < rule.getMinVendorReliabilityScore()) {
                                    validationErrors.add("Selected vendor " + vendor.getVendorName() + " reliability score (" + String.format(java.util.Locale.US, "%.2f", actualScore) + ") is below the required minimum of " + String.format(java.util.Locale.US, "%.2f", rule.getMinVendorReliabilityScore()));
                                }
                            } else {
                                validationErrors.add("Selected quote has no associated vendor");
                            }
                        } else {
                            validationErrors.add("No quote has been selected for the procurement request");
                        }
                    }

                    LocalDateTime entryTime = null;
                    if (currentStep.getWorkflowComponent() != WorkflowComponent.START_EVENT) {
                        entryTime = auditLogRepository
                                .findFirstByRequestAndNewStepOrderByTimestampAsc(request, currentStep)
                                .map(AuditLog::getTimestamp)
                                .orElse(request.getCreatedAt() != null ? request.getCreatedAt()
                                        : LocalDateTime.MIN);
                    }
                    final LocalDateTime finalEntryTime = entryTime;

                    if (rule.getIsPdfRequired() != null && rule.getIsPdfRequired()) {
                        boolean hasPdf = request.getAttachments().stream()
                                .anyMatch(a -> "application/pdf".equalsIgnoreCase(a.getFileType())
                                        && (finalEntryTime == null || a.getUploadedAt().isAfter(finalEntryTime)
                                                || a.getUploadedAt().isEqual(finalEntryTime)));
                        if (!hasPdf)
                            missingAttachments.add("PDF");
                    }
                    if (rule.getIsCsvRequired() != null && rule.getIsCsvRequired()) {
                        boolean hasCsv = request.getAttachments().stream()
                                .anyMatch(a -> "text/csv".equalsIgnoreCase(a.getFileType())
                                        && (finalEntryTime == null || a.getUploadedAt().isAfter(finalEntryTime)
                                                || a.getUploadedAt().isEqual(finalEntryTime)));
                        if (!hasCsv)
                            missingAttachments.add("CSV");
                    }
                    if (rule.getIsImageRequired() != null && rule.getIsImageRequired()) {
                        boolean hasImage = request.getAttachments().stream()
                                .anyMatch(a -> a.getFileType() != null
                                        && a.getFileType().toLowerCase().startsWith("image/")
                                        && (finalEntryTime == null || a.getUploadedAt().isAfter(finalEntryTime)
                                                || a.getUploadedAt().isEqual(finalEntryTime)));
                        if (!hasImage)
                            missingAttachments.add("Image");
                    }

                    if (missingAttachments.size() == 1) {
                        validationErrors.add(missingAttachments.getFirst() + " attachment required");
                    } else if (!missingAttachments.isEmpty()) {
                        validationErrors.add("Missing required attachments: " + String.join(", ", missingAttachments));
                    }

                    if (rule.getAdvancedRule() != null && !rule.getAdvancedRule().isBlank()) {
                        try {
                            ExpressionParser parser = new SpelExpressionParser();
                            EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
                            Boolean isValid = parser.parseExpression(rule.getAdvancedRule())
                                    .getValue(context, new WorkflowBranchingContext(request, currencyConversionService), Boolean.class);
                            if (Boolean.FALSE.equals(isValid)) {
                                validationErrors.add("Advanced validation rule failed: " + rule.getAdvancedRule());
                            }
                        } catch (ExpressionException e) {
                            validationErrors.add("Advanced validation rule failed: " + rule.getAdvancedRule());
                        }
                    }

                    if (!validationErrors.isEmpty()) {
                        throw new WorkflowStateException(buildRuleFailureMessage(validationErrors));
                    }
                }

                WorkflowStep nextStep = transition.getToStep();
                request.setCurrentStep(nextStep);
                request.setRejectionReason(null);
                auditService.createWorkflowTransitionLog(
                        actor,
                        request,
                        transition,
                        APPROVE,
                        "Transitioned from '" + getStepName(transition.getFromStep()) + "' to '" + getStepName(transition.getToStep()) + "'");

                WorkflowComponent componentType = toStep.getWorkflowComponent();

                //Automated Approval
                if (componentType == WorkflowComponent.STEP && Boolean.TRUE.equals(nextStep.getIsAutomatedApproval())) {
                    moveToNextStep(request, null, gatewayDepth + 1, null);
                }

                if (componentType == WorkflowComponent.BRANCH) {
                    moveToNextStep(request, actor, gatewayDepth + 1, nextAssigneeId);
                }

                if (componentType == WorkflowComponent.END_EVENT) {
                    request.setState(RequestStatus.FINISHED);
                    request.setClosedReason(ClosedReason.COMPLETED);
                }

                break;
            }
        }
    }

    @Override
    @Transactional
    public void revertToPreviousStep(Request request, User actor, String reason) {

        WorkflowStep stepToRevertFrom = request.getCurrentStep();

        checkAuthorization(request, actor, stepToRevertFrom);

        WorkflowStep targetStep = null;

        Optional<AuditLog> arrivalLog = auditLogRepository
                .findFirstByRequestAndNewStepAndActionOrderByTimestampDesc(request, stepToRevertFrom, APPROVE);

        if (arrivalLog.isPresent()) {
            targetStep = arrivalLog.get().getPreviousStep();

            while (targetStep != null &&
                    (targetStep.getWorkflowComponent() == WorkflowComponent.BRANCH ||
                            targetStep.getWorkflowComponent() == WorkflowComponent.START_EVENT)) {

                if (targetStep.getWorkflowComponent() == WorkflowComponent.START_EVENT) {
                    break;
                }

                final WorkflowStep branchStep = targetStep;
                targetStep = auditLogRepository
                        .findFirstByRequestAndNewStepAndActionOrderByTimestampDesc(request, branchStep, APPROVE)
                        .map(AuditLog::getPreviousStep)
                        .orElse(null);
            }
        }

        if (targetStep == null) {
            throw new WorkflowStateException(
                    "No valid step found in history to revert to from: " + getStepName(stepToRevertFrom));
        }

        if (targetStep.getWorkflowComponent() == WorkflowComponent.START_EVENT) {
            request.setState(RequestStatus.DRAFT);
            request.setAssignee(null);
        } else {
            Optional<AuditLog> targetDepartureLog = auditLogRepository
                    .findFirstByRequestAndPreviousStepAndActionOrderByTimestampDesc(request, targetStep, APPROVE);
            if (targetDepartureLog.isPresent()) {
                request.setAssignee(targetDepartureLog.get().getActor());
            } else {
                request.setAssignee(null);
            }
        }

        request.setRejectionReason(reason);
        request.setCurrentStep(targetStep);

        auditService.createWorkflowTransitionLog(
                actor,
                request,
                null,
                REVERT,
                "Reverted from '" + getStepName(stepToRevertFrom) + "' to '" + getStepName(targetStep) + "'. Reason: " + reason);

        if (jiraSyncService != null) {
            jiraSyncService.handleVeritasWorkflowChange(request);
        }
    }

    @Override
    public void checkAuthorization(Request request, User actor, WorkflowStep currentStep) {
        if (Boolean.TRUE.equals(currentStep.getIsAutomatedApproval())) {
            return;
        }

        if (request.getAssignee() != null && !request.getAssignee().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Only the assigned user can act on this step.");
        }

        User attachedActor = userRepository.findById(actor.getId()).orElse(actor);

        if (currentStep.getRole() != null) {
            if (Boolean.TRUE.equals(currentStep.getIsTeamLeader())) {
                if (request.getUser() == null || request.getUser().getTeam() == null || request.getUser().getTeam().getLeader() == null
                        || !request.getUser().getTeam().getLeader().getId().equals(attachedActor.getId())) {
                    throw new AccessDeniedException("Only the requester's team leader is authorized for this step.");
                }
            } else if (!attachedActor.getRole().equals(currentStep.getRole())) {
                throw new AccessDeniedException("User with role " + attachedActor.getRole() +
                        " is not authorized for this step. Required: " + currentStep.getRole());
            }
            if (currentStep.getRole() == UserRole.PROCUREMENT_OFFICER) {
                checkReqDeptMatchesUser(request, attachedActor);
            }
        } else {
            if (currentStep.getWorkflowComponent() == WorkflowComponent.START_EVENT) {
                if (request.getUser() != null && !request.getUser().getId().equals(attachedActor.getId())) {
                    throw new AccessDeniedException("Only the creator of the request can submit it.");
                }
                return;
            }

            if (currentStep.getWorkflowComponent() == WorkflowComponent.BRANCH) {
                return;
            }

            if (attachedActor.getRole() == UserRole.REQUESTER) {
                throw new AccessDeniedException("Requesters are not authorized to act on unassigned steps.");
            }
            if (attachedActor.getRole() == UserRole.PROCUREMENT_OFFICER) {
                checkReqDeptMatchesUser(request, attachedActor);
            }
        }
    }

    private void checkReqDeptMatchesUser(Request request, User attachedActor) {
        Long reqDept = null;
        if (request.getUser() != null && request.getUser().getTeam() != null
                && request.getUser().getTeam().getDepartment() != null) {
            reqDept = request.getUser().getTeam().getDepartment().getDepartmentId();
        }
        Long actorDept = attachedActor.getDepartment() != null ? attachedActor.getDepartment().getDepartmentId()
                : null;
        if (reqDept != null && !reqDept.equals(actorDept)) {
            throw new AccessDeniedException("You are not in the same department as the request.");
        }
    }

    private Boolean checkCondition(Request requisition, WorkflowTransition transition) {
        if (transition.getConditionExpression() == null || transition.getConditionExpression().isEmpty()) {
            return true;
        }

        try {
            ExpressionParser parser = new SpelExpressionParser();
            EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
            return parser.parseExpression(transition.getConditionExpression())
                    .getValue(context, new WorkflowBranchingContext(requisition, currencyConversionService), Boolean.class);
        } catch (ExpressionException e) {
            log.warn("Failed to evaluate condition '{}' for request '{}': {}",
                    transition.getConditionExpression(), requisition.getRequestKey(), e.getMessage());
            return false;
        }
    }

    private String buildRuleFailureMessage(List<String> validationErrors) {
        return String.join(", ", validationErrors);
    }

    private void assertBudgetWithinSafetyBuffer(InternalBudget budget) {
        InternalBudget currentBudget = budget;
        while (currentBudget != null) {
            if (currentBudget.getBudgetType() == BudgetType.REQUEST) {
                currentBudget = currentBudget.getParentBudget();
                continue;
            }

            BigDecimal actual = currentBudget.getActualSpend() != null ? currentBudget.getActualSpend()
                    : BigDecimal.ZERO;
            BigDecimal committed = currentBudget.getCommittedSpend() != null ? currentBudget.getCommittedSpend()
                    : BigDecimal.ZERO;
            BigDecimal total = currentBudget.getTotalAmount() != null ? currentBudget.getTotalAmount()
                    : BigDecimal.ZERO;
            BigDecimal safetyBuffer = currentBudget.getSafetyBuffer() != null ? currentBudget.getSafetyBuffer()
                    : BigDecimal.ZERO;

            BigDecimal fraction = safetyBuffer.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal totalWithBuffer = total.multiply(BigDecimal.ONE.subtract(fraction));

            if (actual.add(committed).compareTo(totalWithBuffer) > 0) {
                String budgetIdentifier = currentBudget.getBudgetName();
                if (budgetIdentifier == null || budgetIdentifier.isBlank()) {
                    if (currentBudget.getBudgetType() == BudgetType.PROJECT) {
                        budgetIdentifier = "Project budget";
                    } else if (currentBudget.getBudgetType() == BudgetType.DEPARTMENT) {
                        budgetIdentifier = "Department budget";
                    } else if (currentBudget.getBudgetType() == BudgetType.GLOBAL) {
                        budgetIdentifier = "Global budget";
                    } else {
                        budgetIdentifier = "Budget";
                    }
                }
                throw new WorkflowStateException("Budget of : " + budgetIdentifier + " exhausted including safety buffer.");
            }

            currentBudget = currentBudget.getParentBudget();
        }
    }

    @Override
    @Transactional
    public void startWorkflow(Request request, User actor, Long nextAssigneeId) {

        WorkflowDefinition workflowDef = request.getWorkflowDefinition();

        WorkflowStep startStep = workflowStepRepository
                .findFirstByWorkflowDefinitionAndWorkflowComponent(workflowDef, WorkflowComponent.START_EVENT)
                .orElseThrow(() -> new RuntimeException("No start step configured in workflow"));

        auditService.createWorkflowTransitionLog(
                actor,
                request,
                null,
                "SUBMIT",
                "Request submitted and entered workflow at: '" + getStepName(startStep) +"'");

        this.moveToNextStep(request, actor, nextAssigneeId);
    }

    @Override
    public WorkflowStep getNextStep(Request request) {
        WorkflowStep currentStep = request.getCurrentStep();
        if (request.getState() == RequestStatus.DRAFT) {
            WorkflowDefinition workflowDef = request.getWorkflowDefinition();
            currentStep = workflowStepRepository
                    .findFirstByWorkflowDefinitionAndWorkflowComponent(workflowDef, WorkflowComponent.START_EVENT)
                    .orElse(null);
        }
        if (currentStep == null) {
            return null;
        }
        return resolveNextStep(request, currentStep);
    }

    private List<WorkflowTransition> getPrioritizedTransitions(WorkflowStep step) {
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByFromStep(step);
        if (transitions == null) {
            return List.of();
        }
        return transitions.stream()
                .sorted(Comparator.comparing(
                        t -> t.getConditionExpression() == null || t.getConditionExpression().isEmpty()
                ))
                .toList();
    }

    private WorkflowStep resolveNextStep(Request request, WorkflowStep currentStep) {
        List<WorkflowTransition> transitions = getPrioritizedTransitions(currentStep);
        for (WorkflowTransition transition : transitions) {
            if (checkCondition(request, transition)) {
                WorkflowStep toStep = transition.getToStep();
                if (toStep.getWorkflowComponent() == WorkflowComponent.BRANCH) {
                    return resolveNextStep(request, toStep);
                }
                return toStep;
            }
        }
        return null;
    }

    private String getStepName(WorkflowStep step) {
        if (step == null) {
            return "Unknown";
        }
        if (step.getName() != null && !step.getName().trim().isEmpty()) {
            return step.getName();
        }
        if (step.getWorkflowComponent() != null) {
            switch (step.getWorkflowComponent()) {
                case START_EVENT: return "Start Event";
                case END_EVENT: return "Finished";
                case BRANCH: return "Gateway";
                case STEP: return "Unnamed Step";
            }
        }
        return "Unknown";
    }
}