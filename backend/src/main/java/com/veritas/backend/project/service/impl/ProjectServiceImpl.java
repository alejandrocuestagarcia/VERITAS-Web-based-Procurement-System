package com.veritas.backend.project.service.impl;

import com.veritas.backend.project.dto.ProjectDto;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.project.service.ProjectService;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDto> getProjectsForUser(User user) {
        if (user.getRole() == UserRole.PROCUREMENT_OFFICER || user.getRole() == UserRole.REQUESTER) {
            return projectRepository.findByTeam(user.getTeam()).stream()
                    .map(this::convertProjectToProjectDto)
                    .toList();
        }

        return projectRepository.findAll().stream()
                .map(this::convertProjectToProjectDto)
                .toList();
    }

    private ProjectDto convertProjectToProjectDto(Project project) {
        return new ProjectDto(
                project.getId(),
                project.getName(),
                project.getStartDate(),
                project.getEndDate(),
                project.getBudget(),
                project.getTeam().getName()
        );
    }
}
