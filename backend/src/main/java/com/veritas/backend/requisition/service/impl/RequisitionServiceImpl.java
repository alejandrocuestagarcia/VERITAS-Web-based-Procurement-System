package com.veritas.backend.requisition.service.impl;

import com.veritas.backend.integrations.currency.dto.CurrencyConversionResult;
import com.veritas.backend.integrations.currency.service.CurrencyConversionService;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.integrations.jira.service.JiraSyncService;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.dto.InvoiceCreateDto;
import com.veritas.backend.requisition.dto.InvoiceDto;
import com.veritas.backend.requisition.dto.RequisitionCreateDto;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.dto.RequisitionRejectDto;
import com.veritas.backend.requisition.dto.RequisitionUpdateDto;
import com.veritas.backend.requisition.dto.RequisitionItemCreateDto;
import com.veritas.backend.requisition.entity.*;
import com.veritas.backend.requisition.mapper.InvoiceMapper;
import com.veritas.backend.requisition.mapper.RequisitionMapper;
import com.veritas.backend.requisition.repository.AttachmentRepository;
import com.veritas.backend.requisition.repository.InvoiceRepository;
import com.veritas.backend.requisition.repository.RequestItemRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.requisition.service.RequisitionService;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.repository.QuoteRepository;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.user.mapper.UserMapper;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;

import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.vendor.repository.QuoteLineItemRepository;

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.veritas.backend.audit.service.AuditService;
import java.util.stream.Collectors;

import com.veritas.backend.workflow.service.WorkflowEngineService;
import com.veritas.backend.common.exception.WorkflowStateException;
import org.springframework.security.access.AccessDeniedException;
import com.veritas.backend.notification.service.NotificationService;
import com.veritas.backend.notification.entity.NotificationType;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.veritas.backend.common.model.AuditActionConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionServiceImpl implements RequisitionService {

    private final RequestRepository requestRepository;
    private final ProjectRepository projectRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final UserRepository userRepository;
    private final RequestItemRepository requestItemRepository;
    private final AttachmentRepository attachmentRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final RequisitionMapper requisitionMapper;
    private final InvoiceMapper invoiceMapper;
    private final InternalBudgetRepository internalBudgetRepository;
    private final UserMapper userMapper;
    private final QuoteLineItemRepository quoteLineItemRepository;
    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;


    private final WorkflowEngineService workflowEngineService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final CurrencyConversionService currencyConversionService;

    @Lazy
    private final JiraSyncService jiraSyncService;

    private static final String UPLOAD_DIR = "uploads/requisitions";

    @Override
    @Transactional
    public RequisitionDto createRequest(RequisitionCreateDto createDto, User authUser) {
        User user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        if (user.getTeam() == null) {
            throw new IllegalArgumentException("Requisition cannot be created because you are not assigned to a team.");
        }

        Project project = projectRepository.findById(createDto.projectId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Project not found with ID: " + createDto.projectId()));

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(createDto.workflowDefinitionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow not found with ID: " + createDto.workflowDefinitionId()));

        validateCreationScope(user, project, workflow);

        Request request = new Request();
        request.setRequestName(createDto.requestName());
        request.setDescription(createDto.description());
        request.setProject(project);
        request.setWorkflowDefinition(workflow);
        request.setPriority(createDto.priority());

        // Set initial workflow step
        WorkflowStep startStep = workflowStepRepository.findFirstByWorkflowDefinitionAndWorkflowComponent(workflow, WorkflowComponent.START_EVENT)
                .orElseThrow(() -> new IllegalStateException("Workflow has no START_EVENT step defined"));
        request.setCurrentStep(startStep);

        request.setUser(user);
        request.setTeam(user.getTeam());

        // Initialize Request Budget
        InternalBudget budget = InternalBudget.builder().budgetName(createDto.requestName())
                .totalAmount(BigDecimal.ZERO).parentBudget(project.getInternalBudget()).budgetType(BudgetType.REQUEST).build();
        internalBudgetRepository.save(budget);
        
        request.setBudget(budget);

        project.setRequestCounter(project.getRequestCounter() + 1);
        projectRepository.save(project);

        request.setRequestKey(project.getProjectKey() + "-" + project.getRequestCounter());

        Request savedRequest = requestRepository.save(request);

        if (createDto.items() != null && !createDto.items().isEmpty()) {
            createDto.items().forEach(itemDto -> {
                RequestItem item = RequestItem.builder().request(savedRequest).name(itemDto.name())
                        .quantity(itemDto.quantity()).unit(itemDto.unit()).description(itemDto.description()).build();
                requestItemRepository.save(item);
                savedRequest.getItems().add(item);
            });
        }

        return requisitionMapper.toDto(savedRequest);
    }

    @Override
    public void validateCreationScope(User user, Project project, WorkflowDefinition workflow) {
        if (!project.getIsActive()) {
            throw new IllegalArgumentException("Requisition cannot be created for an inactive project.");
        }

        if (!workflow.getIsActive()) {
            throw new IllegalArgumentException("Requisition cannot be created with an inactive workflow.");
        }

        Team userTeam = user.getTeam();
        Team projectTeam = project.getTeam();
        if (projectTeam == null || projectTeam.getTeamId() == null
                || !Objects.equals(projectTeam.getTeamId(), userTeam.getTeamId())) {
            throw new AccessDeniedException("Project does not belong to the authenticated user's team.");
        }

        Department userDepartment = userTeam.getDepartment();
        Department workflowDepartment = workflow.getDepartment();
        if (workflowDepartment != null) {
            Long userDepartmentId = userDepartment != null ? userDepartment.getDepartmentId() : null;
            if (!Objects.equals(workflowDepartment.getDepartmentId(), userDepartmentId)) {
                throw new AccessDeniedException("Workflow does not belong to the authenticated user's department.");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequisitionDto> getRequests(String status, String search, Long projectId, LocalDate createdFrom, LocalDate createdTo, Long creatorId, User authUser, Pageable pageable) {
        User user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        Long userIdFilter = null;
        Long assigneeIdFilter = null;
        Long teamIdFilter = null;
        Long departmentIdFilter = null;

        String statusFilter = (status != null && !status.isBlank()) ? status.toUpperCase() : null;
        String searchFilter = (search != null && !search.isBlank()) ? search : null;

        String userRole = user.getRole().name();

        if (userRole.equals("REQUESTER")) {
            if (user.getTeam() != null) {
                teamIdFilter = user.getTeam().getTeamId();
                if (user.getTeam().getLeader() != null && user.getTeam().getLeader().getId().equals(user.getId())) {
                    userIdFilter = null;
                } else {
                    userIdFilter = user.getId();
                }
            } else {
                userIdFilter = user.getId();
            }
        } else if (userRole.equals("PROCUREMENT_OFFICER")) {
            if (user.getDepartment() != null) {
                departmentIdFilter = user.getDepartment().getDepartmentId();
            } else {
                departmentIdFilter = -1L;
            }
        }

        if (creatorId != null) {
            if (userRole.equals("REQUESTER")) {
                if (userIdFilter == null) {
                    userIdFilter = creatorId;
                }
            } else {
                userIdFilter = creatorId;
            }
        }

        LocalDateTime createdFromLdt = (createdFrom != null) ? createdFrom.atStartOfDay() : null;
        LocalDateTime createdToLdt = (createdTo != null) ? createdTo.atTime(23, 59, 59, 999999999) : null;

        Page<Request> requests = requestRepository.findFilteredRequests(
                statusFilter, searchFilter, projectId, userIdFilter, assigneeIdFilter, teamIdFilter, departmentIdFilter, createdFromLdt, createdToLdt, pageable);

        return requests.map(requisitionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RequisitionDto getRequestById(Long id, User actor) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Procurement Request with id '" + id + "' not found"));
        checkRequestAccess(request, actor);
        return requisitionMapper.toDto(request);
    }

    @Override
    @Transactional
    public void saveAttachment(Long requestId, MultipartFile file, User actor) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with ID: " + requestId));
        workflowEngineService.checkAuthorization(request, actor, request.getCurrentStep());
        storeAttachment(file, request, null);
    }

    @Override
    @Transactional
    public void saveAttachmentFromInputStream(Long requestId, String originalFilename, String contentType, long size, java.io.InputStream inputStream) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with ID: " + requestId));

        storeFile(inputStream, originalFilename, contentType, size, request, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadAttachment(Long attachmentId, User actor) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found with id " + attachmentId));
        checkRequestAccess(attachment.getRequest(), actor);

        try {
            Path file = Paths.get(attachment.getStoragePath());
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                MediaType mediaType;
                try {
                    mediaType = MediaType.parseMediaType(attachment.getFileType());
                } catch (InvalidMediaTypeException e) {
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
                }

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                        .contentType(mediaType)
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read file: " + attachment.getFileName());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + attachment.getFileName(), e);
        }
    }

    @Override
    @Transactional
    public void deleteAttachment(Long attachmentId, User actor) {
        Attachment attachment = attachmentRepository.findById(attachmentId).orElseThrow(() -> new EntityNotFoundException("Attachment not found with id: " + attachmentId));

        workflowEngineService.checkAuthorization(attachment.getRequest(), actor, attachment.getRequest().getCurrentStep());

        if (attachment.getInvoice() != null) {
            deleteInvoice(attachment.getRequest().getRequestID(), actor);
            return;
        }

        try {
            Path filePath = Paths.get(attachment.getStoragePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete file: " + attachment.getFileName(), e);
        }

        attachmentRepository.delete(attachment);
    }

    @Override
    @Transactional
    public RequisitionDto approveRequest(Long id, User actor, Long nextAssigneeId) {

        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));

        if (request.getState() == RequestStatus.FINISHED) {
            throw new WorkflowStateException("Request " + id + " is already finished and cannot be approved");
        }

        if (request.getState() == RequestStatus.DRAFT) {
            throw new WorkflowStateException("Request " + id + " is in draft and must be submitted");
        }

        if (request.getInvoice() != null && request.getInvoice().getIsPaid()) {
            throw new WorkflowStateException("Request " + id + " has already been paid and cannot be approved");
        }

        request.setRevisionRequired(false);

        String stepApprovedAt = request.getCurrentStep().getName();
        workflowEngineService.moveToNextStep(request, actor, nextAssigneeId);


        Request saved = requestRepository.save(request);

        List<String> notifiedRecipients = new ArrayList<>();
        if (saved.getUser() != null) {
            notificationService.createNotification(
                    saved.getUser(),
                    saved,
                    NotificationType.APPROVED,
                    "Your request '" + saved.getRequestName() + "' has been approved at step '" + stepApprovedAt + "'."
            );
            notifiedRecipients.add(saved.getUser().getEmail() + " (Reason: APPROVED)");
        }
        if (saved.getState() == RequestStatus.FINISHED) {
            if (saved.getUser() != null) {
                notificationService.createNotification(
                        saved.getUser(),
                        saved,
                        NotificationType.FINISHED,
                        "Your request '" + saved.getRequestName() + "' has been completed."
                );
                notifiedRecipients.add(saved.getUser().getEmail() + " (Reason: FINISHED)");
            }
        }
        if (saved.getState() != RequestStatus.FINISHED) {
            if (saved.getAssignee() != null) {
                notificationService.createNotification(
                        saved.getAssignee(),
                        saved,
                        NotificationType.ASSIGNED,
                        "Request '" + saved.getRequestName() + "' requires your action at step '" + (saved.getCurrentStep() != null ? saved.getCurrentStep().getName() : "Unknown") + "'."
                );
                notifiedRecipients.add(saved.getAssignee().getEmail() + " (Reason: ASSIGNED)");
            }
        }

        if (!notifiedRecipients.isEmpty()) {
            auditService.createNotificationLog(actor, saved,
                    "Notifications sent for approval at step '" + stepApprovedAt + "' to:\n- " + String.join("\n- ", notifiedRecipients));
        }

        return requisitionMapper.toDto(saved);
    }

    @Override
    @Transactional
    public RequisitionDto revertRequest(Long id, User actor, RequisitionRejectDto rejectionData) {

        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));

        if (request.getState() == RequestStatus.FINISHED) {
            throw new WorkflowStateException("Request " + id + " is already finished and cannot be reverted");
        }

        if (request.getState() == RequestStatus.DRAFT) {
            throw new WorkflowStateException("Request " + id + " is in draft and cannot be reverted");
        }

        if (request.getInvoice() != null && request.getInvoice().getIsPaid()) {
            throw new WorkflowStateException("Request " + id + " has already been paid and cannot be reverted");
        }

        String stepRevertedFrom = request.getCurrentStep().getName();

        workflowEngineService.revertToPreviousStep(request, actor, rejectionData.getReason());

        if (rejectionData.getRevisionRequired()!=null && rejectionData.getRevisionRequired()) {
            request.setRevisionRequired(true);
        }

        Request savedRequest = requestRepository.save(request);

        List<String> notifiedRecipients = new ArrayList<>();
        String message = "Your request '" + savedRequest.getRequestName() + "' was sent back from step '" + stepRevertedFrom + "'" +
                (rejectionData.getReason() != null && !rejectionData.getReason().isBlank() ? " with the message: " + rejectionData.getReason() : ".");
        notificationService.createNotification(
                savedRequest.getUser(),
                savedRequest,
                NotificationType.REVERTED,
                message
        );
        notifiedRecipients.add(savedRequest.getUser().getEmail() + " (Reason: REVERTED)");
        if (savedRequest.getAssignee() != null) {
            notificationService.createNotification(
                    savedRequest.getAssignee(),
                    savedRequest,
                    NotificationType.ASSIGNED,
                    "Request '" + savedRequest.getRequestName() + "' requires your action after revert."
            );
            notifiedRecipients.add(savedRequest.getAssignee().getEmail() + " (Reason: ASSIGNED)");
        }

        if (!notifiedRecipients.isEmpty()) {
            auditService.createNotificationLog(actor, savedRequest,
                    "Notifications sent for revert from step '" + stepRevertedFrom + "' to:\n- " + String.join("\n- ", notifiedRecipients));
        }

        return requisitionMapper.toDto(savedRequest);
    }

    @Override
    @Transactional
    public RequisitionDto submitRequest(Long id, User actor, Long nextAssigneeId) {

        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));

        if (request.getTeam() == null) {
            throw new IllegalArgumentException("Requisition cannot be submitted because it has no team assigned.");
        }

        if (request.getState() != RequestStatus.DRAFT) {
            throw new WorkflowStateException("Only drafts can be submitted.");
        }

        request.setState(RequestStatus.ACTIVE);
        request.setRevisionRequired(false);

        workflowEngineService.startWorkflow(request, actor, nextAssigneeId);

        Request savedRequest = requestRepository.save(request);

        List<String> notifiedRecipients = new ArrayList<>();
        if (savedRequest.getAssignee() != null) {
            notificationService.createNotification(
                    savedRequest.getAssignee(),
                    savedRequest,
                    NotificationType.SUBMITTED,
                    "New requisition '" + savedRequest.getRequestName() + "' has been submitted and requires your action."
            );
            notifiedRecipients.add(savedRequest.getAssignee().getEmail() + " (Reason: SUBMITTED)");
        }

        if (!notifiedRecipients.isEmpty()) {
            auditService.createNotificationLog(actor, savedRequest,
                    "Notifications sent for submission to:\n- " + String.join("\n- ", notifiedRecipients));
        }

        return requisitionMapper.toDto(savedRequest);
    }

    @Override
    @Transactional
    public RequisitionDto rejectRequest(Long id, User actor, RequisitionRejectDto rejectionData) {

        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));

        if (request.getState() == RequestStatus.FINISHED) {
            throw new WorkflowStateException("Request " + id + " is already finished and cannot be rejected");
        }

        if (request.getInvoice() != null && request.getInvoice().getIsPaid()) {
            throw new WorkflowStateException("Request " + id + " has already been paid and cannot be rejected");
        }

        if (!canAct(id, actor)) {
            throw new AccessDeniedException("Not allowed to access this request");
        }

        request.setDeletedAt(java.time.LocalDateTime.now());
        request.setRejectionReason(rejectionData.getReason());
        request.setState(RequestStatus.FINISHED);
        request.setClosedReason(ClosedReason.REJECTED);

        auditService.createWorkflowTransitionLog(
                actor,
                request,
                null,
                REJECT,
                "Request was rejected with the following reason: " + rejectionData.getReason());

        freeRequestBudget(request);

        Request savedRequest = requestRepository.save(request);

        if (savedRequest.getJiraIssueKey() != null && !savedRequest.getJiraIssueKey().isBlank()) {
            jiraSyncService.handleVeritasWorkflowChange(savedRequest);
        }

        String message = "Your request '" + savedRequest.getRequestName() + "' was rejected" +
                (rejectionData.getReason() != null && !rejectionData.getReason().isBlank() ? " with the message: " + rejectionData.getReason() : ".");
        notificationService.createNotification(
                savedRequest.getUser(),
                savedRequest,
                NotificationType.REJECTED,
                message
        );
        auditService.createNotificationLog(actor, savedRequest,
                "Notifications sent for rejection to:\n- " + savedRequest.getUser().getEmail() + " (Reason: REJECTED)");

        return requisitionMapper.toDto(savedRequest);
    }

    @Override
    @Transactional
    public RequisitionDto cancelRequest(Long id, User actor) {

        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));

        if (request.getState() == RequestStatus.FINISHED) {
            throw new WorkflowStateException("Finished requests cannot be cancelled");
        }

        if (request.getState() != RequestStatus.DRAFT && !canAct(id, actor)) {
            throw new WorkflowStateException("Only requests in Draft or where the current step is assigned to you can be cancelled");
        }

        if (actor.getRole() != UserRole.REQUESTER || !actor.getId().equals(request.getUser().getId())) {
            throw new AccessDeniedException("Only the creator of the request can cancel it");
        }

        request.setDeletedAt(java.time.LocalDateTime.now());
        request.setState(RequestStatus.FINISHED);
        request.setClosedReason(ClosedReason.CANCELLED);

        auditService.createWorkflowTransitionLog(
                actor,
                request,
                null,
                CANCEL,
                "Request was cancelled by " + actor.getName());

        freeRequestBudget(request);

        Request savedRequest = requestRepository.save(request);

        return requisitionMapper.toDto(savedRequest);
    }

    @Override
    @Transactional
    public RequisitionDto changeRequester(Long id, Long newRequesterId, User actor) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));

        if (request.getState() == RequestStatus.FINISHED) {
            throw new WorkflowStateException("Finished requests cannot be reassigned");
        }

        if (newRequesterId == null) {
            throw new IllegalArgumentException("New assigned user must be stated");
        }

        User newRequester = userRepository.findById(newRequesterId)
                .orElseThrow(() -> new EntityNotFoundException("New assigned user not found"));

        if (newRequester.getRole() != UserRole.REQUESTER || !newRequester.getTeam().getTeamId().equals(request.getUser().getTeam().getTeamId())) {
            throw new IllegalArgumentException("New assigned user must be a requester from the same team");
        }

        User oldRequester = request.getUser();
        request.setUser(newRequester);
        Request updatedRequest = requestRepository.save(request);
        if (jiraSyncService != null) {
            jiraSyncService.handleVeritasWorkflowChange(updatedRequest);
        }

        auditService.createWorkflowTransitionLog(
                actor, request, null, REQUESTER_CHANGED,
                "Requester changed from " + (oldRequester != null ? oldRequester.getName() : "unknown")
                        + " to " + newRequester.getName());

        List<String> notifiedRecipients = new ArrayList<>();
        notificationService.createNotification(
                oldRequester,
                updatedRequest,
                NotificationType.REASSIGNED,
                "Request '" + updatedRequest.getRequestName() + "' has been reassigned to " + newRequester.getName() + ". You will no longer receive notifications for this request."
        );
        notifiedRecipients.add(oldRequester.getEmail() + " (Reason: REASSIGNED)");
        notificationService.createNotification(
                newRequester,
                updatedRequest,
                NotificationType.ASSIGNED,
                "Request '" + updatedRequest.getRequestName() + "' has been assigned to you."
        );
        notifiedRecipients.add(newRequester.getEmail() + " (Reason: ASSIGNED)");

        auditService.createNotificationLog(actor, updatedRequest,
                "Notifications sent for requester change to:\n- " + String.join("\n- ", notifiedRecipients));

        return requisitionMapper.toDto(updatedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public String getNextStepRole(Long id, User actor) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));
        checkRequestAccess(request, actor);
        WorkflowStep nextStep = workflowEngineService.getNextStep(request);
        if (nextStep != null && nextStep.getRole() != null && !nextStep.getIsAutomatedApproval()) {
            return nextStep.getRole().name();
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getEligibleAssignees(Long id, String roleName, User actor) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));
        checkRequestAccess(request, actor);
        UserRole requiredRole = UserRole.valueOf(roleName);

        Long requestDepartmentId = null;
        if (request.getUser().getTeam() != null) {
            requestDepartmentId = request.getUser().getTeam().getDepartment().getDepartmentId();
        }

        final Long finalReqDeptId = requestDepartmentId;
        return userRepository.findAllByRoleAndIsActiveTrue(requiredRole).stream()
                .filter(u -> {
                    if (requiredRole == UserRole.FINANCE_OFFICER || requiredRole == UserRole.ADMINISTRATOR) {
                        return true;
                    }
                    if (finalReqDeptId == null) {
                        return true;
                    }
                    if (requiredRole == UserRole.PROCUREMENT_OFFICER) {
                        return u.getDepartment() != null && u.getDepartment().getDepartmentId().equals(finalReqDeptId);
                    }
                    if (requiredRole == UserRole.REQUESTER) {
                        return u.getTeam() != null && u.getTeam().getDepartment() != null &&
                               u.getTeam().getDepartment().getDepartmentId().equals(finalReqDeptId);
                    }
                    return false;
                })
                .map(userMapper::toUserDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAct(Long id, User actor) {
        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) {
            return false;
        }
        try {
            workflowEngineService.checkAuthorization(request, actor, request.getCurrentStep());
            return true;
        } catch (AccessDeniedException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public RequisitionDto updateRequest(Long id, RequisitionUpdateDto updates, User actor) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Procurement Request with id '" + id + "' not found"));

        if (request.getState() != RequestStatus.DRAFT && !request.getRevisionRequired()) {
            throw new WorkflowStateException("Only requests that are in DRAFT or need a Revision can be edited");
        }

        boolean isCreator = request.getUser().getId().equals(actor.getId());
        boolean isAuthorizedForCurrentStep = false;
        if (request.getCurrentStep() != null) {
            try {
                workflowEngineService.checkAuthorization(request, actor, request.getCurrentStep());
                isAuthorizedForCurrentStep = true;
            } catch (AccessDeniedException e) {
                // Actor is not authorized for the current active step
            }
        }

        if (!isCreator && !(request.getRevisionRequired() && isAuthorizedForCurrentStep)) {
            throw new AccessDeniedException("You are not authorized to edit this request");
        }

        List<String> changes = new ArrayList<>();
        if (!Objects.equals(request.getRequestName(), updates.requestName())) {
            changes.add("Request Name changed from '" + request.getRequestName() + "' to '" + updates.requestName() + "'");
        }
        if (!Objects.equals(request.getDescription(), updates.description())) {
            String oldDesc = request.getDescription();
            String newDesc = updates.description();
            if (oldDesc == null || oldDesc.isEmpty()) {
                if (newDesc != null && !newDesc.isEmpty()) {
                    changes.add("Description set to '" + newDesc + "'");
                }
            } else {
                if (newDesc == null || newDesc.isEmpty()) {
                    changes.add("Description cleared (was '" + oldDesc + "')");
                } else {
                    changes.add("Description changed from '" + oldDesc + "' to '" + newDesc + "'");
                }
            }
        }
        if (!Objects.equals(request.getPriority(), updates.priority())) {
            changes.add("Priority changed from '" + request.getPriority() + "' to '" + updates.priority() + "'");
        }
        if (updates.projectId() != null && !request.getProject().getId().equals(updates.projectId())) {
            String oldName = request.getProject().getName();
            Project newProject = projectRepository.findById(updates.projectId()).orElse(null);
            String newName = newProject != null ? newProject.getName() : String.valueOf(updates.projectId());
            changes.add("Project changed from '" + oldName + "' to '" + newName + "'");
        }
        WorkflowDefinition newWorkflow = null;
        boolean workflowChanged = updates.workflowDefinitionId() != null
                && !request.getWorkflowDefinition().getId().equals(updates.workflowDefinitionId());
        if (workflowChanged) {
            String oldName = request.getWorkflowDefinition().getName();
            newWorkflow = workflowDefinitionRepository.findById(updates.workflowDefinitionId())
                    .orElseThrow(() -> new IllegalArgumentException("Workflow not found with ID: " + updates.workflowDefinitionId()));
            String newName = newWorkflow.getName();
            changes.add("Workflow changed from '" + oldName + "' to '" + newName + "'");
        }

        boolean itemsChanged = hasLineItemsChanged(request.getItems(), updates.items());
        if (itemsChanged) {
            String oldItemsStr = request.getItems().stream()
                    .map(item -> item.getName() + " (" + item.getQuantity() + " " + item.getUnit() + (item.getDescription() != null && !item.getDescription().isEmpty() ? " - " + item.getDescription() : "") + ")")
                            .collect(Collectors.joining(", "));
            String newItemsStr = updates.items() == null ? "" : updates.items().stream()
                    .map(item -> item.name() + " (" + item.quantity() + " " + item.unit() + (item.description() != null && !item.description().isEmpty() ? " - " + item.description() : "") + ")")
                            .collect(Collectors.joining(", "));
            changes.add("Line Items changed from '" + oldItemsStr + "' to '" + newItemsStr + "'");
        }

        if (!changes.isEmpty()) {
            String changeDetails = String.join("\n", changes);
            auditService.createRequisitionChangeLog(actor, request, changeDetails);
        }

        request.setRequestName(updates.requestName());
        request.setDescription(updates.description());
        request.setPriority(updates.priority());

        if (updates.projectId() != null && !request.getProject().getId().equals(updates.projectId())) {
            Project newProject = projectRepository.findById(updates.projectId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Project not found with ID: " + updates.projectId()));

            newProject.setRequestCounter(newProject.getRequestCounter() + 1);
            projectRepository.save(newProject);
            request.setRequestKey(newProject.getProjectKey() + "-" + newProject.getRequestCounter());
            request.setProject(newProject);

            request.getBudget().setParentBudget(newProject.getInternalBudget());
        }

        request.getBudget().setBudgetName(updates.requestName());
        internalBudgetRepository.save(request.getBudget());

        if (workflowChanged) {
            request.setWorkflowDefinition(newWorkflow);
            WorkflowStep startStep = workflowStepRepository
                    .findFirstByWorkflowDefinitionAndWorkflowComponent(newWorkflow, WorkflowComponent.START_EVENT)
                    .orElseThrow(() -> new IllegalStateException("Workflow has no START_EVENT step defined"));
            request.setCurrentStep(startStep);
            request.setAssignee(request.getUser());
            request.setState(RequestStatus.DRAFT);
        }

        if (itemsChanged) {
            deleteInvoiceQuoteChanged(request);

            if (request.getSelectedQuote() != null) {
                freeRequestBudget(request);
            }

            quoteLineItemRepository.deleteByQuoteRequestID(id);
            quoteRepository.deleteByRequestID(id);

            request.getItems().clear();
            requestItemRepository.deleteByRequestID(id);

            if (updates.items() != null && !updates.items().isEmpty()) {
                updates.items().forEach(itemDto -> {
                    RequestItem item = RequestItem.builder().request(request).name(itemDto.name())
                            .quantity(itemDto.quantity()).unit(itemDto.unit()).description(itemDto.description()).build();
                    requestItemRepository.save(item);
                    request.getItems().add(item);
                });
            }
        }

        Request saved = requestRepository.save(request);
        return requisitionMapper.toDto(saved);
    }

    private boolean hasLineItemsChanged(List<RequestItem> currentItems, List<RequisitionItemCreateDto> incomingItems) {
        if (incomingItems == null) {
            return currentItems != null && !currentItems.isEmpty();
        }
        if (currentItems.size() != incomingItems.size()) {
            return true;
        }

        for (int i = 0; i < currentItems.size(); i++) {
            RequestItem current = currentItems.get(i);
            RequisitionItemCreateDto incoming = incomingItems.get(i);

            String currentDesc = current.getDescription() == null ? "" : current.getDescription().trim();
            String incomingDesc = incoming.description() == null ? "" : incoming.description().trim();

            if (!Objects.equals(current.getName(), incoming.name()) ||
                    !Objects.equals(current.getQuantity(), incoming.quantity()) ||
                    !Objects.equals(current.getUnit(), incoming.unit()) ||
                    !currentDesc.equals(incomingDesc)) {
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional
    public void processPayment(Long requestId, User actor) {

        Request request = requestRepository.findById(requestId).orElseThrow(
                () -> new EntityNotFoundException("Request not found with id: " + requestId)
        );

        if (request.getClosedReason() == ClosedReason.REJECTED || request.getClosedReason() == ClosedReason.CANCELLED) {
            throw new WorkflowStateException("Cannot pay a request that has been " + request.getClosedReason());
        }

        Invoice invoice = request.getInvoice();
        if (invoice == null) {
            throw new EntityNotFoundException("Invoice not found for request with id: " + requestId);
        }

        addToBudgets(request.getBudget(), invoice);
        invoice.setIsPaid(true);
        invoiceRepository.save(invoice);

        request.setState(RequestStatus.FINISHED);
        Request saved = requestRepository.save(request);

        notificationService.createNotification(
                saved.getUser(),
                saved,
                NotificationType.PAID,
                "Payment has been processed for your request '" + saved.getRequestName() + "'."
        );
        auditService.createNotificationLog(actor, saved,
                "Notifications sent for payment to:\n- " + saved.getUser().getEmail() + " (Reason: PAID)");

        auditService.createWorkflowTransitionLog(
                actor,
                request,
                null,
                PAID,
                "Request was paid by " + actor.getName());

        if (saved.getJiraIssueKey() != null && !saved.getJiraIssueKey().isBlank()) {
            jiraSyncService.handleVeritasWorkflowChange(saved);
        }
    }

    private void addToBudgets(InternalBudget budget, Invoice invoice) {
        BigDecimal requestCommittedSpent = budget.getCommittedSpend() != null ? budget.getCommittedSpend() : BigDecimal.ZERO;
        if (invoice.getTotalAmount() == null) {
            throw new IllegalStateException("Invoice has no Total amount defined");
        }

        CurrencyConversionResult conversion = currencyConversionService.convert(invoice.getTotalAmount(), invoice.getCurrency());
        BigDecimal invoiceAmountEur = conversion.convertedAmount();
        invoice.setPaidAmountEur(invoiceAmountEur);

        BigDecimal difference = invoiceAmountEur.subtract(requestCommittedSpent);
        validateBudget(budget, difference, false);

        while (budget != null) {
            BigDecimal newTotalSpend = budget.getActualSpend().add(invoiceAmountEur);
            budget.setActualSpend(newTotalSpend);

            budget.setCommittedSpend(budget.getCommittedSpend().subtract(requestCommittedSpent));

            internalBudgetRepository.save(budget);
            budget = budget.getParentBudget();
        }
    }

    public void validateBudget(InternalBudget budget, BigDecimal difference, boolean includesSafetyBuffer) {
        InternalBudget currentBudget = budget;
        while (currentBudget != null) {
            if (currentBudget.getBudgetType() == BudgetType.REQUEST) {
                currentBudget = currentBudget.getParentBudget();
                continue;
            }

            BigDecimal actual = currentBudget.getActualSpend();
            BigDecimal committed = currentBudget.getCommittedSpend();
            BigDecimal total = currentBudget.getTotalAmount() != null ? currentBudget.getTotalAmount()
                    : BigDecimal.ZERO;
            BigDecimal safetyBuffer = currentBudget.getSafetyBuffer();

            BigDecimal threshold = total;
            if (includesSafetyBuffer && safetyBuffer.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal fraction = safetyBuffer.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
                threshold = total.multiply(BigDecimal.ONE.subtract(fraction));
            }

            BigDecimal simulatedTotal = actual.add(committed).add(difference);

            if (simulatedTotal.compareTo(threshold) > 0) {
                String budgetIdentifier = currentBudget.getBudgetName();
                if (budgetIdentifier == null || budgetIdentifier.isBlank()) {
                    switch (currentBudget.getBudgetType()) {
                        case PROJECT -> budgetIdentifier = "Project budget";
                        case DEPARTMENT -> budgetIdentifier = "Department budget";
                        case GLOBAL -> budgetIdentifier = "Global budget";
                        default -> budgetIdentifier = "Budget";
                    }
                }
                String messageSuffix = includesSafetyBuffer ? " exhausted including safety buffer." : " exhausted.";
                throw new WorkflowStateException("Budget of " + budgetIdentifier + messageSuffix);
            }

            currentBudget = currentBudget.getParentBudget();
        }
    }

    private void freeRequestBudget(Request request) {
        if (request.getBudget() == null) {
            return;
        }
        BigDecimal requestCommittedSpent = request.getBudget().getCommittedSpend() != null 
                ? request.getBudget().getCommittedSpend() : BigDecimal.ZERO;
        InternalBudget budget = request.getBudget();
        while (budget != null) {
            BigDecimal currentCommitted = budget.getCommittedSpend() != null ? budget.getCommittedSpend() : BigDecimal.ZERO;
            budget.setCommittedSpend(currentCommitted.subtract(requestCommittedSpent));
            internalBudgetRepository.save(budget);
            budget = budget.getParentBudget();
        }
    }

    @Override
    @Transactional
    public InvoiceDto createInvoice(Long requestId, InvoiceCreateDto createDto, MultipartFile file, User actor) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + requestId));

        workflowEngineService.checkAuthorization(request, actor, request.getCurrentStep());

        if (request.getInvoice() != null) {
            throw new EntityExistsException("Invoice already exists for request with id: " + requestId);
        }

        Quote selectedQuote = request.getSelectedQuote();
        if (selectedQuote == null) {
            throw new IllegalStateException("No vendor quote has been selected for this request. Please select a quote first.");
        }

        Invoice invoice = new Invoice();
        invoice.setRequest(request);
        invoice.setVendor(selectedQuote.getVendorID());
        invoice.setInvoiceNumber(createDto.getInvoiceNumber());
        invoice.setInvoiceDate(createDto.getInvoiceDate());
        invoice.setTotalAmount(createDto.getTotalAmount());
        invoice.setCurrency(createDto.getCurrency());
        invoice.setDueDate(createDto.getDueDate());
        invoice.setIsPaid(false);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        if (file != null && !file.isEmpty()) {
            storeAttachment(file, request, savedInvoice);
        }

        return this.mapToInvoiceDtoWithEuro(savedInvoice);
    }

    @Override
    @Transactional
    public InvoiceDto updateInvoice(Long requestId, InvoiceCreateDto updateDto, MultipartFile file, User actor) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + requestId));

        workflowEngineService.checkAuthorization(request, actor, request.getCurrentStep());

        Invoice invoice = request.getInvoice();
        if (invoice == null) {
            throw new EntityNotFoundException("Invoice not found for request with id: " + requestId);
        }

        if (invoice.getIsPaid()) {
            throw new IllegalStateException("Cannot edit an invoice that has already been paid.");
        }

        invoice.setInvoiceNumber(updateDto.getInvoiceNumber());
        invoice.setInvoiceDate(updateDto.getInvoiceDate());
        invoice.setTotalAmount(updateDto.getTotalAmount());
        invoice.setCurrency(updateDto.getCurrency());
        invoice.setDueDate(updateDto.getDueDate());

        if (file != null && !file.isEmpty()) {
            List<Attachment> attachments = new ArrayList<>(invoice.getAttachments());
            for (Attachment attachment : attachments) {
                try {
                    Path filePath = Paths.get(attachment.getStoragePath());
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    throw new RuntimeException("Could not delete file: " + attachment.getFileName(), e);
                }
                request.getAttachments().remove(attachment);
                attachmentRepository.delete(attachment);
            }
            invoice.getAttachments().clear();
            storeAttachment(file, request, invoice);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return this.mapToInvoiceDtoWithEuro(savedInvoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto getInvoice(Long requestId, User actor) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + requestId));
        checkRequestAccess(request, actor);

        Invoice invoice = request.getInvoice();
        if (invoice == null) {
            throw new EntityNotFoundException("Invoice not found for request with id: " + requestId);
        }

        return this.mapToInvoiceDtoWithEuro(invoice);
    }

    @Override
    @Transactional
    public void deleteInvoice(Long requestId, User user) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + requestId));

        workflowEngineService.checkAuthorization(request, user, request.getCurrentStep());

        Invoice invoice = request.getInvoice();
        if (invoice == null) {
            throw new EntityNotFoundException("Invoice not found for request with id: " + requestId);
        }

        if (invoice.getIsPaid()) {
            throw new IllegalStateException("Cannot delete an invoice that has already been paid.");
        }

        // Delete attachment files and entities
        for (Attachment attachment : invoice.getAttachments()) {
            try {
                Path filePath = Paths.get(attachment.getStoragePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Could not delete file: " + attachment.getFileName(), e);
            }
            request.getAttachments().remove(attachment);
            attachmentRepository.delete(attachment);
        }

        request.setInvoice(null);
        invoiceRepository.delete(invoice);
    }

    private InvoiceDto mapToInvoiceDtoWithEuro(Invoice invoice) {
        BigDecimal totalAmountEuro;
        CurrencyConversionResult conversion = null;

        try {
            conversion = currencyConversionService.convert(invoice.getTotalAmount(), invoice.getCurrency());
            totalAmountEuro = conversion.convertedAmount();
        } catch (IllegalArgumentException | EntityNotFoundException exception) {
            log.warn("Could not convert invoice {} amount {} {} to EUR: {}", invoice.getInvoiceId(), invoice.getTotalAmount(), invoice.getCurrency(), exception.getMessage());
            totalAmountEuro = null;
        }

        return invoiceMapper.toDto(invoice, totalAmountEuro, conversion != null ? conversion.fetchedAt() : null, conversion != null ? conversion.source() : null);
    }

    private void storeAttachment(MultipartFile file, Request request, Invoice invoice) {
        try {
            storeFile(file.getInputStream(), file.getOriginalFilename(), file.getContentType(), file.getSize(), request, invoice);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    private void storeFile(java.io.InputStream inputStream, String originalFilename, String contentType, long size, Request request, Invoice invoice) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String storedFilename = UUID.randomUUID().toString() + extension;
            Path targetLocation = uploadPath.resolve(storedFilename);

            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);

            Attachment attachment = new Attachment();
            attachment.setRequest(request);
            attachment.setInvoice(invoice);
            attachment.setFileName(originalFilename);
            attachment.setFileType(contentType);
            attachment.setFileSize(size);
            attachment.setStoragePath(targetLocation.toString());
            attachment.setUploadedAt(LocalDateTime.now());
            attachmentRepository.save(attachment);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    @Override
    public void checkRequestAccess(Long requestId, User user) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + requestId));
        checkRequestAccess(request, user);
    }

    private void checkRequestAccess(Request request, User user) {
        if (user.getRole() == UserRole.REQUESTER) {
            User fullUser = userRepository.findById(user.getId()).orElseThrow(() -> new EntityNotFoundException("User not found with id: " + user.getId()));
            if (fullUser.equals(fullUser.getTeam().getLeader())) {
                if (!request.getTeam().getTeamId().equals(fullUser.getTeam().getTeamId())) {
                    throw new AccessDeniedException("Not allowed to access this request");
                }
                return;
            }
            if (!request.getUser().getId().equals(fullUser.getId())) {
                throw new AccessDeniedException("Not allowed to access this request");
            }
        } else if (user.getRole() == UserRole.PROCUREMENT_OFFICER) {
            if (!user.getDepartment().getDepartmentId().equals(request.getTeam().getDepartment().getDepartmentId())) {
                throw new AccessDeniedException("Not allowed to access this request");
            }
        }
    }

    private void deleteInvoiceQuoteChanged(Request request) {
        Invoice invoice = request.getInvoice();
        if (invoice != null) {
            for (Attachment attachment : invoice.getAttachments()) {
                try {
                    Path filePath = Paths.get(attachment.getStoragePath());
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    log.error("Could not delete file: " + attachment.getFileName(), e);
                }
                request.getAttachments().remove(attachment);
                attachmentRepository.delete(attachment);
            }
            request.setInvoice(null);
            invoiceRepository.delete(invoice);
        }
    }
}
