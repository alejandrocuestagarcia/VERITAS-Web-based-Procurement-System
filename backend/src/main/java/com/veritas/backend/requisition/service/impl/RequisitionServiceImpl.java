package com.veritas.backend.requisition.service.impl;

import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.dto.RequisitionCreateDto;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.entity.Attachment;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestItem;
import com.veritas.backend.requisition.mapper.RequisitionMapper;
import com.veritas.backend.requisition.repository.AttachmentRepository;
import com.veritas.backend.requisition.repository.RequestItemRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.requisition.service.RequisitionService;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RequisitionServiceImpl implements RequisitionService {

    private final RequestRepository requestRepository;
    private final ProjectRepository projectRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final UserRepository userRepository;
    private final RequestItemRepository requestItemRepository;
    private final AttachmentRepository attachmentRepository;
    private final RequisitionMapper requisitionMapper;

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
        request.setUserID(user);
        request.setTeamID(user.getTeam());

        project.setRequestCounter(project.getRequestCounter() + 1);
        projectRepository.save(project);

        request.setRequestKey(project.getProjectKey() + "-" + project.getRequestCounter());

        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        Request savedRequest = requestRepository.save(request);

        if (createDto.items() != null && !createDto.items().isEmpty()) {
            createDto.items().forEach(itemDto -> {
                RequestItem item = new RequestItem();
                item.setRequest(savedRequest);
                item.setName(itemDto.name());
                item.setQuantity(itemDto.quantity());
                item.setEstimatedPrice(itemDto.estimatedPrice());
                item.setDescription(itemDto.description());
                requestItemRepository.save(item);
                savedRequest.getItems().add(item);
            });
        }

        return requisitionMapper.toDto(savedRequest);
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
}
