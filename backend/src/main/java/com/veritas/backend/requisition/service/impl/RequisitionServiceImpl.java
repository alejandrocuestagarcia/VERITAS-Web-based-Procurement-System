package com.veritas.backend.requisition.service.impl;

import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.dto.RequisitionCreateDto;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.mapper.RequisitionMapper;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.requisition.service.RequisitionService;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RequisitionServiceImpl implements RequisitionService {

    private final RequestRepository requestRepository;
    private final ProjectRepository projectRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final UserRepository userRepository;
    private final RequisitionMapper requisitionMapper;

    @Override
    @Transactional
    public RequisitionDto createRequest(RequisitionCreateDto createDto, User authUser) {
        User user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        Project project = projectRepository.findById(createDto.getProjectId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Project not found with ID: " + createDto.getProjectId()));

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(createDto.getWorkflowDefinitionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow not found with ID: " + createDto.getWorkflowDefinitionId()));

        Request request = new Request();
        request.setRequestName(createDto.getRequestName());
        request.setDescription(createDto.getDescription());
        request.setProjectID(project);
        request.setWorkflowDefinitionID(workflow);
        request.setPriority(createDto.getPriority());
        request.setUserID(user);
        request.setTeamID(user.getTeam());

        project.setRequestCounter(project.getRequestCounter() + 1);
        projectRepository.save(project);

        request.setRequestKey(project.getProjectKey() + "-" + project.getRequestCounter());

        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        Request savedRequest = requestRepository.save(request);
        return requisitionMapper.toDto(savedRequest);
    }
}
