package com.veritas.backend.project.service.impl;

import java.time.LocalDateTime;

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
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.department.entity.Department;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

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
    public List<ProjectDto> getProjectsForUser(User user, boolean includeInactive) {
        log.debug("Fetching projects for user: {} (role={}), includeInactive={}", user.getEmail(), user.getRole(), includeInactive);

        if (user.getRole() == UserRole.REQUESTER) {
            if (user.getTeam() == null) {
                log.warn("Requester {} has no team assigned; returning no projects", user.getEmail());
                return List.of();
            }
            List<ProjectDto> teamProjects = (includeInactive ? projectRepository.findByTeam(user.getTeam()) : projectRepository.findByTeamAndIsActiveTrue(user.getTeam())).stream()
                    .map(projectMapper::toProjectDto)
                    .toList();
            log.debug("Returning {} team-scoped projects for user: {}", teamProjects.size(), user.getEmail());
            return teamProjects;
        }

        if (user.getRole() == UserRole.PROCUREMENT_OFFICER) {
            if (user.getDepartment() == null) {
                log.warn("Procurement officer {} has no department assigned; returning no projects", user.getEmail());
                return List.of();
            }
            List<ProjectDto> departmentProjects = (includeInactive ? projectRepository.findByTeamDepartment(user.getDepartment()) : projectRepository.findByTeamDepartmentAndIsActiveTrue(user.getDepartment())).stream()
                    .map(projectMapper::toProjectDto)
                    .toList();
            log.debug("Returning {} department-scoped projects for user: {}", departmentProjects.size(), user.getEmail());
            return departmentProjects;
        }

        List<ProjectDto> allProjects = (includeInactive ? projectRepository.findAll() : projectRepository.findByIsActiveTrue()).stream()
                .map(projectMapper::toProjectDto)
                .toList();
        log.debug("Returning {} projects for user: {}", allProjects.size(), user.getEmail());
        return allProjects;
    }

    private void validateProjectBudgetLimit(Department department, Long excludeProjectId, BigDecimal projectBudget) {
        if (department != null && department.getInternalBudget() != null) {
            BigDecimal deptLimit = department.getInternalBudget().getTotalAmount();
            if (deptLimit != null) {
                BigDecimal existingTotal = projectRepository.findByTeamDepartment(department).stream()
                        .filter(p -> excludeProjectId == null || !p.getId().equals(excludeProjectId))
                        .map(p -> p.getInternalBudget().getTotalAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal newTotal = existingTotal.add(projectBudget);
                if (newTotal.compareTo(deptLimit) > 0) {
                    log.warn("Project budget check failed – budget limit exceeded for department: {} (Limit: {}, Attempted: {})", 
                            department.getName(), deptLimit, newTotal);
                    DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.GERMANY));
                    throw new IllegalArgumentException("Project budget of " + df.format(projectBudget) + "€"
                            + " exceeds the remaining department budget of " + df.format(deptLimit.subtract(existingTotal)) + "€"
                            + " (Total Limit: " + df.format(deptLimit) + "€)");
                }
            }
        }
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
        validateProjectBudgetLimit(department, null, projectCreationDto.budget());

        Project project = projectMapper.toProject(projectCreationDto);
        project.setTeam(team);

        // Initialize budget
        InternalBudget budget = new InternalBudget();
        budget.setBudgetName(project.getName());
        budget.setTotalAmount(projectCreationDto.budget());
        budget.setBudgetType(BudgetType.PROJECT);
        if (department != null && department.getInternalBudget() != null) {
            budget.setParentBudget(department.getInternalBudget());
        }

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

        if (!project.getIsActive()) {
            throw new IllegalStateException("Cannot edit a deactivated project");
        }

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

            project.setEndDate(updatedProject.endDate());
        }

        if (updatedProject.budget() != null || updatedProject.teamId() != null) {
            Team targetTeam = updatedProject.teamId() != null ? 
                    teamRepository.findById(updatedProject.teamId())
                            .orElseThrow(() -> new EntityNotFoundException("Team with id " + updatedProject.teamId() + " not found")) :
                    project.getTeam();
            BigDecimal targetBudget = updatedProject.budget() != null ? updatedProject.budget() : project.getInternalBudget().getTotalAmount();
            validateProjectBudgetLimit(targetTeam.getDepartment(), id, targetBudget);
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
            if (project.getInternalBudget() != null) {
                project.getInternalBudget().setParentBudget(team.getDepartment() != null ? team.getDepartment().getInternalBudget() : null);
            }
        }
        Project saved = projectRepository.save(project);
        log.info("Project edited successfully – id: {}", saved.getId());
        return projectMapper.toProjectDto(saved);
    }

    @Override
    @Transactional
    public boolean deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id " + id));

        if (jiraConfigRepository.existsByFallbackProjectId(id)) {
            throw new IllegalStateException("Cannot delete project because it is used as a fallback project in a Jira configuration");
        }

        if (requestRepository.existsActiveRequisitionsByProjectId(id)) {
            throw new IllegalStateException("Cannot delete project because it has open or in-progress requisitions");
        }

        if (requestRepository.existsByProjectId(id)) {
            // Soft-delete: all requisitions are finished, deactivate the project for audit/history
            project.setIsActive(false);
            project.setDeactivatedAt(LocalDateTime.now());
            projectRepository.save(project);
            log.info("Project soft-deleted (deactivated) – id: {}", id);
            return true;
        } else {
            // Hard-delete: no requisitions reference this project
            projectRepository.delete(project);
            log.info("Project hard-deleted – id: {}", id);
            return false;
        }
    }
}
