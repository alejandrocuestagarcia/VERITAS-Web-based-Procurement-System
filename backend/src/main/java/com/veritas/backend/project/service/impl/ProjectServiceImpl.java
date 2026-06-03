package com.veritas.backend.project.service.impl;

import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.project.dto.ProjectCreationDto;
import com.veritas.backend.project.dto.ProjectDto;
import com.veritas.backend.project.dto.ProjectEditDto;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.mapper.ProjectMapper;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.project.service.ProjectService;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.integrations.jira.repository.JiraConfigRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.department.entity.Department;
import java.math.BigDecimal;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final ProjectMapper projectMapper;
    private final RequestRepository requestRepository;
    private final JiraConfigRepository jiraConfigRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDto> getProjectsForUser(User user) {
        log.debug("Fetching projects for user: {} (role={})", user.getEmail(), user.getRole());

        if (user.getRole() == UserRole.REQUESTER) {
            List<ProjectDto> teamProjects = projectRepository.findByTeam(user.getTeam()).stream()
                    .map(projectMapper::toProjectDto)
                    .toList();
            log.debug("Returning {} team-scoped projects for user: {}", teamProjects.size(), user.getEmail());
            return teamProjects;
        }

        if (user.getRole() == UserRole.PROCUREMENT_OFFICER) {
            List<ProjectDto> departmentProjects = projectRepository.findByTeamDepartment(user.getDepartment()).stream()
                    .map(projectMapper::toProjectDto)
                    .toList();
            log.debug("Returning {} department-scoped projects for user: {}", departmentProjects.size(), user.getEmail());
            return departmentProjects;
        }

        List<ProjectDto> allProjects = projectRepository.findAll().stream()
                .map(projectMapper::toProjectDto)
                .toList();
        log.debug("Returning all {} projects for user: {}", allProjects.size(), user.getEmail());
        return allProjects;
    }

    @Override
    @Transactional
    public ProjectDto createProject(ProjectCreationDto projectCreationDto) {
        log.info("Creating project – name: {}, key: {}", projectCreationDto.name(), projectCreationDto.projectKey());

        if (projectRepository.existsByNameOrProjectKey(projectCreationDto.name(), projectCreationDto.projectKey())) {
            log.warn("Project creation failed – duplicate name or key: name='{}', key='{}'", projectCreationDto.name(), projectCreationDto.projectKey());
            throw new EntityExistsException("Project with same name or projectKey already exists");
        }

        Team team = teamRepository.findById(projectCreationDto.teamId())
                .orElseThrow(() -> new EntityNotFoundException("Team with id " + projectCreationDto.teamId() + " not found"));
        log.debug("Assigned project to team: {} (id={})", team.getName(), team.getTeamId());

        // Department Budget Check
        Department department = team.getDepartment();
        if (department != null && department.getInternalBudget() != null) {
            BigDecimal deptLimit = department.getInternalBudget().getTotalAmount();
            if (deptLimit != null) {
                BigDecimal existingTotal = projectRepository.findByTeamDepartment(department).stream()
                        .map(p -> p.getInternalBudget() != null ? p.getInternalBudget().getTotalAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal newTotal = existingTotal.add(projectCreationDto.budget());
                if (newTotal.compareTo(deptLimit) > 0) {
                    log.warn("Project creation blocked – budget limit exceeded for department: {} (Limit: {}, Attempted: {})", 
                            department.getName(), deptLimit, newTotal);
                    throw new IllegalArgumentException("Project budget of " + projectCreationDto.budget() 
                            + " exceeds the remaining department budget of " + deptLimit.subtract(existingTotal) 
                            + " (Total Limit: " + deptLimit + ")");
                }
            }
        }

        Project project = projectMapper.toProject(projectCreationDto);
        project.setTeam(team);

        // Initialize budget
        InternalBudget budget = new InternalBudget();
        budget.setBudgetName("Project: " + project.getName());
        budget.setTotalAmount(projectCreationDto.budget());
        project.setInternalBudget(budget);

        Project saved = projectRepository.save(project);

        log.info("Project created successfully – id: {}, name: {}", saved.getId(), saved.getName());
        return projectMapper.toProjectDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id,User user) {
        log.debug("Fetching project by id: {}", id);

        if (user.getRole() == UserRole.REQUESTER) {

            Project project = projectRepository.findByIdAndTeam(id,user.getTeam())
                    .orElseThrow(() -> new EntityNotFoundException("Project with id " + id + " not found"));
            return projectMapper.toProjectDto(project);
        }

        if (user.getRole() == UserRole.PROCUREMENT_OFFICER) {
            Project project = projectRepository.findByIdAndTeamDepartment(id,user.getDepartment())
                    .orElseThrow(() -> new EntityNotFoundException("Project with id " + id + " not found"));
            return projectMapper.toProjectDto(project);
        }

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project with id " + id + " not found"));
        return projectMapper.toProjectDto(project);
    }

    @Override
    @Transactional
    public ProjectDto editProject(Long id, ProjectEditDto updatedProject) {
        log.info("Editing project id: {}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project with id " + id + " not found"));

        if (updatedProject.startDate() != null && !project.getStartDate().equals(updatedProject.startDate())) {

            if(project.getStartDate().isBefore(LocalDate.now())){
                throw new IllegalArgumentException("Start date can only be changed if the project has not started already");
            }
            if (updatedProject.startDate().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Start date can only be changed if the new date is in the future");
            }
            project.setStartDate(updatedProject.startDate());
        }
        if (updatedProject.endDate() != null && !project.getEndDate().equals(updatedProject.endDate())) {

            if (updatedProject.endDate().isBefore(updatedProject.startDate())) {
                throw new IllegalArgumentException("End date must be after start date");
            }

            if (updatedProject.endDate().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("End date can only be changed if the new date is in the future");
            }

            project.setEndDate(updatedProject.startDate());
        }

        if(updatedProject.name()!= null) project.setName(updatedProject.name());
        if(updatedProject.budget()!= null){
            if (project.getInternalBudget() == null) {
                throw new EntityNotFoundException("No internal budget is currently assigned to project id: " + project.getId());
            }

            project.getInternalBudget().setTotalAmount(updatedProject.budget());
        }
        if(updatedProject.teamId()!=null) {
            Team team = teamRepository.findById(updatedProject.teamId())
                    .orElseThrow(() -> new EntityNotFoundException("Team with id " + updatedProject.teamId() + " not found"));
            project.setTeam(team);
        }
        Project saved = projectRepository.save(project);
        log.info("Project edited successfully – id: {}", saved.getId());
        return projectMapper.toProjectDto(saved);
    }

    @Override
    @Transactional
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new EntityNotFoundException("Project not found with id " + id);
        }

        if (requestRepository.existsByProjectId(id)) {
            throw new IllegalStateException("Cannot delete project because there are requisitions pointing to it");
        }

        if (jiraConfigRepository.existsByFallbackProjectId(id)) {
            throw new IllegalStateException("Cannot delete project because it is used as a fallback project in a Jira configuration");
        }

        projectRepository.deleteById(id);
    }
}

