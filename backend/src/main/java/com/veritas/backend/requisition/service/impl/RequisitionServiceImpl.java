package com.veritas.backend.requisition.service.impl;

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
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.vendor.repository.QuoteLineItemRepository;
import java.math.BigDecimal;

import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.veritas.backend.audit.service.AuditService;
import java.util.stream.Collectors;

import com.veritas.backend.workflow.service.WorkflowEngineService;
import com.veritas.backend.common.exception.WorkflowStateException;
import org.springframework.security.access.AccessDeniedException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.veritas.backend.common.model.AuditActionConstants.PAID;

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

    @Lazy
    private final JiraSyncService jiraSyncService;

    private static final String UPLOAD_DIR = "uploads/requisitions";

    @Override
    @Transactional
    public RequisitionDto createRequest(RequisitionCreateDto createDto, User authUser) {
        User user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        Project project = projectRepository.findById(createDto.projectId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Project not found with ID: " + createDto.projectId()));

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(createDto.workflowDefinitionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow not found with ID: " + createDto.workflowDefinitionId()));

        Request request = new Request();
        request.setRequestName(createDto.requestName());
        request.setDescription(createDto.description());
        request.setProjectID(project);
        request.setWorkflowDefinitionID(workflow);
        request.setPriority(createDto.priority());

        // Set initial workflow step
        WorkflowStep startStep = workflowStepRepository.findFirstByWorkflowDefinitionAndWorkflowComponent(workflow, WorkflowComponent.START_EVENT)
                .orElseThrow(() -> new IllegalStateException("Workflow has no START_EVENT step defined"));
        request.setCurrentStepID(startStep);

        request.setUserID(user);
        request.setTeamID(user.getTeam());

        // Initialize Request Budget
        InternalBudget budget = new InternalBudget();
        budget.setBudgetName("Request: " + createDto.requestName());
        budget.setTotalAmount(BigDecimal.ZERO);
        budget.setParentBudget(project.getInternalBudget());
        internalBudgetRepository.save(budget);
        
        request.setBudgetID(budget);

        project.setRequestCounter(project.getRequestCounter() + 1);
        projectRepository.save(project);

        request.setRequestKey(project.getProjectKey() + "-" + project.getRequestCounter());

        Request savedRequest = requestRepository.save(request);

        if (createDto.items() != null && !createDto.items().isEmpty()) {
            createDto.items().forEach(itemDto -> {
                RequestItem item = new RequestItem();
                item.setRequest(savedRequest);
                item.setName(itemDto.name());
                item.setQuantity(itemDto.quantity());
                item.setUnit(itemDto.unit());
                item.setDescription(itemDto.description());
                requestItemRepository.save(item);
                savedRequest.getItems().add(item);
            });
        }

        return requisitionMapper.toDto(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequisitionDto> getRequests(String status, String search, Long projectId, User authUser, Pageable pageable) {
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
            }
            userIdFilter = user.getId();
        } else if (userRole.equals("PROCUREMENT_OFFICER")) {
            if (user.getDepartment() != null) {
                departmentIdFilter = user.getDepartment().getDepartmentId();
            } else {
                departmentIdFilter = -1L;
            }
        }

        Page<Request> requests = requestRepository.findFilteredRequests(
                statusFilter, searchFilter, projectId, userIdFilter, assigneeIdFilter, teamIdFilter, departmentIdFilter, pageable);

        return requests.map(requisitionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RequisitionDto getRequestById(Long id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Procurement Request with id '" + id + "' not found"));
        return requisitionMapper.toDto(request);
    }

    @Override
    @Transactional
    public void saveAttachment(Long requestId, MultipartFile file) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with ID: " + requestId));

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
    public ResponseEntity<Resource> downloadAttachment(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found with id " + attachmentId));

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
    public void deleteAttachment(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId).orElseThrow(() -> new EntityNotFoundException("Attachment not found with id: " + attachmentId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        if (user.getRole() == UserRole.REQUESTER && !attachment.getRequest().getUserID().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not allowed to access this request");
        }

        if (user.getRole() == UserRole.PROCUREMENT_OFFICER && !attachment.getRequest().getTeamID().getDepartment().getDepartmentId().equals(user.getDepartment().getDepartmentId())) {
            throw new AccessDeniedException("Not allowed to access this request");
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

        if (request.getInvoice() != null && Boolean.TRUE.equals(request.getInvoice().getIsPaid())) {
            throw new WorkflowStateException("Request " + id + " has already been paid and cannot be approved");
        }

        workflowEngineService.moveToNextStep(request, actor, nextAssigneeId);


        Request saved = requestRepository.save(request);

        return requisitionMapper.toDto(saved);
    }

    @Override
    @Transactional
    public RequisitionDto rejectRequest(Long id, User actor, RequisitionRejectDto rejectionData) {

        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));

        if (request.getState() == RequestStatus.FINISHED) {
            throw new WorkflowStateException("Request " + id + " is already finished and cannot be rejected");
        }

        if (request.getState() == RequestStatus.DRAFT) {
            throw new WorkflowStateException("Request " + id + " is in draft and cannot be rejected");
        }

        if (request.getInvoice() != null && Boolean.TRUE.equals(request.getInvoice().getIsPaid())) {
            throw new WorkflowStateException("Request " + id + " has already been paid and cannot be rejected");
        }

        workflowEngineService.revertToPreviousStep(request, actor, rejectionData.getReason());

        Request savedRequest = requestRepository.save(request);


        return requisitionMapper.toDto(savedRequest);
    }

    @Override
    @Transactional
    public RequisitionDto submitRequest(Long id, User actor, Long nextAssigneeId) {

        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));

        if (request.getState() != RequestStatus.DRAFT) {
            throw new WorkflowStateException("Only drafts can be submitted.");
        }

        request.setState(RequestStatus.ACTIVE);

        workflowEngineService.startWorkflow(request, actor, nextAssigneeId);

        Request savedRequest = requestRepository.save(request);
        return requisitionMapper.toDto(savedRequest);
    }

    @Override
    @Transactional
    public RequisitionDto changeRequester(Long id, Long newRequesterId) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));

        if (newRequesterId == null) {
            throw new IllegalArgumentException("New assigned user must be stated");
        }

        User newRequester = userRepository.findById(newRequesterId)
                .orElseThrow(() -> new EntityNotFoundException("New assigned user not found"));

        if (newRequester.getRole() != UserRole.REQUESTER || !newRequester.getTeam().getTeamId().equals(request.getUserID().getTeam().getTeamId())) {
            throw new IllegalArgumentException("New assigned user must be a requester from the same team");
        }

        request.setUserID(newRequester);
        Request updatedRequest = requestRepository.save(request);
        if (jiraSyncService != null) {
            jiraSyncService.handleVeritasWorkflowChange(updatedRequest);
        }
        
        return requisitionMapper.toDto(updatedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public String getNextStepRole(Long id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));
        WorkflowStep nextStep = workflowEngineService.getNextStep(request);
        if (nextStep != null && nextStep.getRole() != null) {
            return nextStep.getRole().name();
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getEligibleAssignees(Long id, String roleName) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));
        UserRole requiredRole = UserRole.valueOf(roleName);

        Long requestDepartmentId = null;
        if (request.getUserID() != null && request.getUserID().getTeam() != null && request.getUserID().getTeam().getDepartment() != null) {
            requestDepartmentId = request.getUserID().getTeam().getDepartment().getDepartmentId();
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
        if (request == null || request.getCurrentStepID() == null) {
            return false;
        }
        try {
            workflowEngineService.checkAuthorization(request, actor, request.getCurrentStepID());
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

        if (request.getState() != RequestStatus.DRAFT) {
            throw new WorkflowStateException("Only drafts can be updated/edited");
        }

        if (!request.getUserID().getId().equals(actor.getId())) {
            throw new AccessDeniedException("You are not authorized to edit this request");
        }

        List<String> changes = new ArrayList<>();
        if (!Objects.equals(request.getRequestName(), updates.requestName())) {
            changes.add("Field 'requestName' changed from '" + request.getRequestName() + "' to '" + updates.requestName() + "'");
        }
        if (!Objects.equals(request.getDescription(), updates.description())) {
            changes.add("Field 'description' changed from '" + request.getDescription() + "' to '" + updates.description() + "'");
        }
        if (!Objects.equals(request.getPriority(), updates.priority())) {
            changes.add("Field 'priority' changed from '" + request.getPriority() + "' to '" + updates.priority() + "'");
        }
        if (updates.projectId() != null && (request.getProjectID() == null || !request.getProjectID().getId().equals(updates.projectId()))) {
            Long oldId = request.getProjectID() != null ? request.getProjectID().getId() : null;
            changes.add("Field 'projectId' changed from '" + oldId + "' to '" + updates.projectId() + "'");
        }
        if (updates.workflowDefinitionId() != null && (request.getWorkflowDefinitionID() == null || !request.getWorkflowDefinitionID().getId().equals(updates.workflowDefinitionId()))) {
            Long oldId = request.getWorkflowDefinitionID() != null ? request.getWorkflowDefinitionID().getId() : null;
            changes.add("Field 'workflowDefinitionId' changed from '" + oldId + "' to '" + updates.workflowDefinitionId() + "'");
        }

        boolean itemsChanged = hasLineItemsChanged(request.getItems(), updates.items());
        if (itemsChanged) {
            String oldItemsStr = request.getItems() == null ? "" : request.getItems().stream()
                    .map(item -> item.getName() + " (" + item.getQuantity() + " " + item.getUnit() + (item.getDescription() != null && !item.getDescription().isEmpty() ? " - " + item.getDescription() : "") + ")")
                    .collect(Collectors.joining(", "));
            String newItemsStr = updates.items() == null ? "" : updates.items().stream()
                    .map(item -> item.name() + " (" + item.quantity() + " " + item.unit() + (item.description() != null && !item.description().isEmpty() ? " - " + item.description() : "") + ")")
                    .collect(Collectors.joining(", "));
            changes.add("Field 'items' changed from '" + oldItemsStr + "' to '" + newItemsStr + "'");
        }

        if (!changes.isEmpty()) {
            String changeDetails = String.join("\n", changes);
            auditService.createRequisitionChangeLog(actor, request, changeDetails);
        }

        request.setRequestName(updates.requestName());
        request.setDescription(updates.description());
        request.setPriority(updates.priority());

        if (updates.projectId() != null && !request.getProjectID().getId().equals(updates.projectId())) {
            Project newProject = projectRepository.findById(updates.projectId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Project not found with ID: " + updates.projectId()));

            newProject.setRequestCounter(newProject.getRequestCounter() + 1);
            projectRepository.save(newProject);
            request.setRequestKey(newProject.getProjectKey() + "-" + newProject.getRequestCounter());
            request.setProjectID(newProject);

            if (request.getBudgetID() != null) {
                request.getBudgetID().setParentBudget(newProject.getInternalBudget());
            }
        }

        if (request.getBudgetID() != null) {
            request.getBudgetID().setBudgetName("Request: " + updates.requestName());
            internalBudgetRepository.save(request.getBudgetID());
        }

        if (updates.workflowDefinitionId() != null
                && !request.getWorkflowDefinitionID().getId().equals(updates.workflowDefinitionId())) {
            WorkflowDefinition newWorkflow = workflowDefinitionRepository.findById(updates.workflowDefinitionId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Workflow not found with ID: " + updates.workflowDefinitionId()));

            request.setWorkflowDefinitionID(newWorkflow);
            WorkflowStep startStep = workflowStepRepository
                    .findFirstByWorkflowDefinitionAndWorkflowComponent(newWorkflow, WorkflowComponent.START_EVENT)
                    .orElseThrow(() -> new IllegalStateException("Workflow has no START_EVENT step defined"));
            request.setCurrentStepID(startStep);
        }

        if (itemsChanged) {
            quoteLineItemRepository.deleteByQuoteRequestID(id);
            quoteRepository.deleteByRequestID(id);

            request.getItems().clear();
            requestItemRepository.deleteByRequestID(id);

            if (updates.items() != null && !updates.items().isEmpty()) {
                updates.items().forEach(itemDto -> {
                    RequestItem item = new RequestItem();
                    item.setRequest(request);
                    item.setName(itemDto.name());
                    item.setQuantity(itemDto.quantity());
                    item.setUnit(itemDto.unit());
                    item.setDescription(itemDto.description());
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
        if (currentItems == null || currentItems.size() != incomingItems.size()) {
            return true;
        }

        for (int i = 0; i < currentItems.size(); i++) {
            RequestItem current = currentItems.get(i);
            RequisitionItemCreateDto incoming = incomingItems.get(i);

            if (!Objects.equals(current.getName(), incoming.name()) ||
                    !Objects.equals(current.getQuantity(), incoming.quantity()) ||
                    !Objects.equals(current.getUnit(), incoming.unit()) ||
                    !Objects.equals(current.getDescription(), incoming.description())) {
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

        Invoice invoice = request.getInvoice();
        if (invoice == null) {
            throw new EntityNotFoundException("Invoice not found for request with id: " + requestId);
        }

        if (request.getBudgetID() != null) {
            addToBudgets(request.getBudgetID(), invoice);
            invoice.setIsPaid(true);
            invoiceRepository.save(invoice);
        }

        request.setState(RequestStatus.FINISHED);
        Request saved = requestRepository.save(request);

        auditService.createWorkflowTransitionLog(
                actor,
                request,
                null,
                PAID,
                "Transitioned was paid by " + actor.getName());

        if (saved.getJiraIssueKey() != null && !saved.getJiraIssueKey().isBlank()) {
            jiraSyncService.handleVeritasWorkflowChange(saved);
        }
    }

    private void addToBudgets(InternalBudget budget, Invoice invoice) {


        BigDecimal requestCommittedSpent = budget.getCommittedSpend();
        while (budget != null) {

            if (invoice.getTotalAmount() == null) {
                throw new IllegalStateException("Invoice has no Total amount defined");
            }

            BigDecimal newTotalSpend = budget.getActualSpend().add(invoice.getTotalAmount());
            budget.setActualSpend(newTotalSpend);

            budget.setCommittedSpend(budget.getCommittedSpend().subtract(requestCommittedSpent));


            internalBudgetRepository.save(budget);
            budget = budget.getParentBudget();


        }

    }

    @Override
    @Transactional
    public InvoiceDto createInvoice(Long requestId, InvoiceCreateDto createDto, MultipartFile file) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + requestId));

        if (request.getInvoice() != null) {
            throw new EntityExistsException("Invoice already exists for request with id: " + requestId);
        }

        var selectedQuote = request.getQuotes().stream()
                .filter(Quote::isSelected)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No vendor quote has been selected for this request. Please select a quote first."));

        Invoice invoice = new Invoice();
        invoice.setRequest(request);
        invoice.setVendor(selectedQuote.getVendorID());
        invoice.setInvoiceNumber(createDto.getInvoiceNumber());
        invoice.setInvoiceDate(createDto.getInvoiceDate());
        invoice.setTotalAmount(createDto.getTotalAmount());
        invoice.setDueDate(createDto.getDueDate());
        invoice.setIsPaid(false);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        if (file != null && !file.isEmpty()) {
            storeAttachment(file, request, savedInvoice);
        }

        return invoiceMapper.toDto(savedInvoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto getInvoice(Long requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + requestId));

        Invoice invoice = request.getInvoice();
        if (invoice == null) {
            throw new EntityNotFoundException("Invoice not found for request with id: " + requestId);
        }

        return invoiceMapper.toDto(invoice);
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
}
