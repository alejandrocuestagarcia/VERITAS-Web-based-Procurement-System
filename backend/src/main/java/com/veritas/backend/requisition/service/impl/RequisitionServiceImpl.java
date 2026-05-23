package com.veritas.backend.requisition.service.impl;

import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.dto.RequisitionCreateDto;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.dto.RequisitionRejectDto;
import com.veritas.backend.requisition.entity.Attachment;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestItem;
import com.veritas.backend.requisition.mapper.RequisitionMapper;
import com.veritas.backend.requisition.repository.AttachmentRepository;
import com.veritas.backend.requisition.repository.RequestItemRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.requisition.entity.RequestStatus;
import com.veritas.backend.requisition.service.RequisitionService;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.user.mapper.UserMapper;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import java.math.BigDecimal;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
import java.util.UUID;
import java.util.List;

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
    private final InternalBudgetRepository internalBudgetRepository;
    private final UserMapper userMapper;

    private final WorkflowEngineService workflowEngineService;

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
        WorkflowStep startStep = workflowStepRepository.findByWorkflowDefinitionAndWorkflowComponent(workflow, WorkflowComponent.START_EVENT)
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
                statusFilter, searchFilter, projectId, userIdFilter, assigneeIdFilter, teamIdFilter, departmentIdFilter, WorkflowComponent.END_EVENT, pageable);

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

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String storedFilename = UUID.randomUUID().toString() + extension;
            Path targetLocation = uploadPath.resolve(storedFilename);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            Attachment attachment = new Attachment();
            attachment.setRequest(request);
            attachment.setFileName(originalFilename);
            attachment.setFileType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setStoragePath(targetLocation.toString());
            attachment.setUploadedAt(LocalDateTime.now());

            attachmentRepository.save(attachment);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + file.getOriginalFilename() + ". Please try again!", ex);
        }
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
                } catch (Exception e) {
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
    public RequisitionDto approveRequest(Long id, User actor, Long nextAssigneeId) {

        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + id));

        if (request.getState() == RequestStatus.FINISHED) {
            throw new WorkflowStateException("Request " + id + " is already finished and cannot be approved");
        }

        if (request.getState() == RequestStatus.DRAFT) {
            throw new WorkflowStateException("Request " + id + " is in draft and must be submitted");
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
}
