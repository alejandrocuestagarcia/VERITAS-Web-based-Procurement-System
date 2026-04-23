package com.veritas.backend.project.service.impl;

import com.veritas.backend.project.dto.ProjectCreationDto;
import com.veritas.backend.project.dto.ProjectDto;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.mapper.ProjectMapper;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.project.service.ProjectService;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;

import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final ProjectMapper projectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDto> getProjectsForUser(User user) {
        if (user.getRole() == UserRole.PROCUREMENT_OFFICER || user.getRole() == UserRole.REQUESTER) {
            return projectRepository.findByTeam(user.getTeam()).stream()
                    .map(projectMapper::toProjectDto)
                    .toList();
        }

        return projectRepository.findAll().stream()
                .map(projectMapper::toProjectDto)
                .toList();
    }

    @Override
    @Transactional
    public ProjectDto createProject(ProjectCreationDto projectCreationDto) {

        if (projectRepository.existsByNameOrProjectKey(projectCreationDto.name(), projectCreationDto.projectKey())) {
            throw new EntityExistsException("Project with same name or projectKey already exists");
        }

        // TODO: Restore this once the team feature is fully implemented.
       // Team team = teamRepository.findById(project.teamId()).orElseThrow(() -> new EntityNotFoundException("Team with id " + project.teamId() + " not found"));

        // Fallback: Pick the first available team since team feature is incomplete
        java.util.List<Team> teams = teamRepository.findAll();
        if (teams.isEmpty()) {
            throw new IllegalStateException("Cannot create project: No teams available in the system.");
        }
        Team team = teams.getFirst();

        Project project = projectMapper.toProject(projectCreationDto);
        project.setTeam(team);
        Project saved = projectRepository.save(project);

        return projectMapper.toProjectDto(saved);
    }
}
