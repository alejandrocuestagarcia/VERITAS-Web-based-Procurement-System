package com.veritas.backend.workflow.service.impl;

import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.entity.WorkflowTransition;

import com.veritas.backend.user.entity.User;

import com.veritas.backend.audit.entity.AuditLog;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.audit.service.impl.AuditServiceImpl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.workflow.repository.WorkflowTransitionRepository;
import com.veritas.backend.workflow.repository.TransitionRuleRepository;
import com.veritas.backend.workflow.mapper.WorkflowMapper;
import com.veritas.backend.workflow.service.WorkflowEngineService;


import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.repository.QuoteRepository;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestStatus;


import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import com.veritas.backend.common.exception.WorkflowStateException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;


import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class WorkflowEngineServiceImpl implements WorkflowEngineService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final TransitionRuleRepository transitionRuleRepository;
    private final QuoteRepository quoteRepository;
    private final WorkflowMapper workflowMapper;
    private final AuditServiceImpl auditService;
    private final AuditLogRepository auditLogRepository;


    @Override
    @Transactional
    public void moveToNextStep(Request request, User actor) {
        WorkflowStep currentStep = request.getCurrentStepID();

        if (currentStep.getRole() != null) {
            if (!actor.getRole().equals(currentStep.getRole())) {
                throw new AccessDeniedException("User with role " + actor.getRole() +
                        " is not authorized to approve this step. Required: " + currentStep.getRole());
            }
        }

        List<WorkflowTransition> transitions = workflowTransitionRepository.findByFromStep(currentStep);

        for (WorkflowTransition transition : transitions) {
            if (checkCondition(request,transition)) {
                
                InternalBudget budget = request.getBudgetID();
                if (budget != null) {
                    BigDecimal actual = budget.getActualSpend() != null ? budget.getActualSpend() : BigDecimal.ZERO;
                    BigDecimal committed = budget.getCommittedSpend() != null ? budget.getCommittedSpend() : BigDecimal.ZERO;
                    BigDecimal total = budget.getTotalAmount() != null ? budget.getTotalAmount() : BigDecimal.ZERO;
                    BigDecimal safetyBuffer = budget.getSafetyBuffer() != null ? budget.getSafetyBuffer() : BigDecimal.ZERO;
                    
                    BigDecimal fraction = safetyBuffer.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                    BigDecimal totalWithBuffer = total.multiply(BigDecimal.ONE.subtract(fraction));
                    
                    if (actual.add(committed).compareTo(totalWithBuffer) > 0) {
                        throw new WorkflowStateException("Budget exhausted including safety buffer.");
                    }
                }

                Optional<TransitionRule> optRule = transitionRuleRepository.findByTransition(transition);
                if (optRule.isPresent()) {
                    TransitionRule rule = optRule.get();
                    
                    if (rule.getMinRequiredVendors() != null && rule.getMinRequiredVendors() > 0) {
                        Long requestId = request.getRequestID();
                        List<Quote> quotes = requestId != null
                            ? quoteRepository.findByRequestRequestID(requestId)
                            : List.of();
                        long distinctVendors = quotes.stream()
                            .map(Quote::getVendorID)
                            .filter(Objects::nonNull)
                            .map(Vendor::getId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .count();
                        if (distinctVendors < rule.getMinRequiredVendors()) {
                            throw new WorkflowStateException(rule.getOptionalFailureMessage() != null ? rule.getOptionalFailureMessage() : "Not enough vendors");
                        }
                    }
                    
                    if (rule.getIsPdfRequired() != null && rule.getIsPdfRequired()) {
                        boolean hasPdf = request.getAttachments().stream()
                                .anyMatch(a -> "application/pdf".equalsIgnoreCase(a.getFileType()));
                        if (!hasPdf) throw new WorkflowStateException(rule.getOptionalFailureMessage() != null ? rule.getOptionalFailureMessage() : "PDF attachment required");
                    }
                    if (rule.getIsCsvRequired() != null && rule.getIsCsvRequired()) {
                        boolean hasCsv = request.getAttachments().stream()
                                .anyMatch(a -> "text/csv".equalsIgnoreCase(a.getFileType()));
                        if (!hasCsv) throw new WorkflowStateException(rule.getOptionalFailureMessage() != null ? rule.getOptionalFailureMessage() : "CSV attachment required");
                    }
                    if (rule.getIsImageRequired() != null && rule.getIsImageRequired()) {
                        boolean hasImage = request.getAttachments().stream()
                                .anyMatch(a -> a.getFileType() != null && a.getFileType().toLowerCase().startsWith("image/"));
                        if (!hasImage) throw new WorkflowStateException(rule.getOptionalFailureMessage() != null ? rule.getOptionalFailureMessage() : "Image attachment required");
                    }
                }

                request.setCurrentStepID(transition.getToStep());

                auditService.createWorkflowTransitionLog(
                        actor,
                        request,
                        transition,
                        "APPROVE",
                        "Transitioned from " + transition.getFromStep().getName() + " to " + transition.getToStep().getName()
                );

                WorkflowComponent componentType = transition.getToStep().getWorkflowComponent();
                if (componentType == WorkflowComponent.BRANCH) {
                    moveToNextStep(request, actor);
                }

                if (componentType == WorkflowComponent.END_EVENT) {
                    request.setState(RequestStatus.FINISHED);
                }

                break;
            }
        }
    }

    @Override
    @Transactional
    public void revertToPreviousStep(Request request, User actor, String reason) {

        WorkflowStep stepToRevertFrom = request.getCurrentStepID();

        if (stepToRevertFrom.getRole() != null) {
            if (!actor.getRole().equals(stepToRevertFrom.getRole())) {
                throw new AccessDeniedException("User with role " + actor.getRole() +
                        " is not authorized to reject this step. Required: " + stepToRevertFrom.getRole());
            }
        }

        WorkflowStep targetStep = null;

        Optional<AuditLog> arrivalLog = auditLogRepository
                .findFirstByRequestAndNewStepAndActionOrderByTimestampDesc(request, stepToRevertFrom, "APPROVE");

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
                        .findFirstByRequestAndNewStepAndActionOrderByTimestampDesc(request, branchStep, "APPROVE")
                        .map(AuditLog::getPreviousStep)
                        .orElse(null);
            }
        }

        if (targetStep == null) {
            throw new WorkflowStateException("No valid step found in history to revert to from: " + stepToRevertFrom.getName());
        }

        if (targetStep.getWorkflowComponent() == WorkflowComponent.START_EVENT) {
            request.setState(RequestStatus.DRAFT);
        }

        request.setCurrentStepID(targetStep);

        auditService.createWorkflowTransitionLog(
                actor,
                request,
                null,
                "REVERT",
                "Reverted from " + stepToRevertFrom.getName() + " to " + targetStep.getName() + ". Reason: " + reason
        );

    }

    private Boolean checkCondition(Request requisition, WorkflowTransition transition){
        if (transition.getConditionExpression() == null || transition.getConditionExpression().isEmpty()) {
            return true;
        }

        try {
            ExpressionParser parser = new SpelExpressionParser();
            EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
            return parser.parseExpression(transition.getConditionExpression())
                    .getValue(context, requisition, Boolean.class);
        } catch (Exception e) {
            return false;
        }
    }

    public void startWorkflow(Request request, User actor) {

        WorkflowDefinition workflowDef = request.getWorkflowDefinitionID();

        WorkflowStep startStep = workflowStepRepository
                .findByWorkflowDefinitionAndWorkflowComponent(workflowDef, WorkflowComponent.START_EVENT)
                .orElseThrow(() -> new RuntimeException("No start step configured in workflow"));

        auditService.createWorkflowTransitionLog(
                actor,
                request,
                null,
                "SUBMIT",
                "Request submitted and entered workflow at: " + startStep.getName()
        );

        this.moveToNextStep(request, actor);
    }

}