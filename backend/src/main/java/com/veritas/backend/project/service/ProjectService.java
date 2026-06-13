package com.veritas.backend.project.service;

import com.veritas.backend.project.dto.ProjectCreationDto;
import com.veritas.backend.project.dto.ProjectDto;
import com.veritas.backend.project.dto.ProjectEditDto;
import com.veritas.backend.user.entity.User;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

import java.util.List;

public interface ProjectService {

    /**
     * Returns all projects visible to the given user based on their role.
     * Requesters see only their team's projects, procurement officers see their department's projects,
     * and all other roles see every project.
     *
     * @param user the authenticated user
     * @return a list of accessible {@link ProjectDto}
     */
    List<ProjectDto> getProjectsForUser(User user);

    /**
     * Creates a new project with an initialized internal budget, assigned to the specified team.
     * Validates that the new project budget does not exceed the department's remaining budget.
     *
     * @param projectDto the project creation payload
     * @return the created {@link ProjectDto}
     * @throws EntityExistsException if a project with the same name or key already exists
     * @throws EntityNotFoundException if the specified team does not exist
     * @throws IllegalArgumentException if the budget would exceed the department limit
     */
    ProjectDto createProject(ProjectCreationDto projectDto);

    /**
     * Returns a single project by ID, scoped to what the given user is allowed to see.
     *
     * @param id the project ID
     * @param user the authenticated user
     * @return the matching {@link ProjectDto}
     * @throws EntityNotFoundException if the project does not exist or is not accessible to the user
     */
    ProjectDto getProjectById(Long id,User user);

    /**
     * Updates an existing project.
     * Start and end date changes are validated against the current date and each other.
     *
     * @param id the ID of the project to edit
     * @param updatedProject the edit payload
     * @return the updated {@link ProjectDto}
     * @throws EntityNotFoundException if the project or specified team does not exist
     * @throws IllegalArgumentException if the date changes violate business rules
     */
    ProjectDto editProject(Long id, ProjectEditDto updatedProject);

    /**
     * Deletes a project by ID. Deletion is blocked if any requisitions or Jira configurations reference it.
     *
     * @param id the ID of the project to delete
     * @throws EntityNotFoundException if no project exists with the given ID
     * @throws IllegalStateException if the project is referenced by requisitions or a Jira fallback configuration
     */
    void deleteProject(Long id);
}
