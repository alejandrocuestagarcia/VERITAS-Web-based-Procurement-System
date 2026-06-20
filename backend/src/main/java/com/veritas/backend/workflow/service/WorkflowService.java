package com.veritas.backend.workflow.service;

import com.veritas.backend.workflow.dto.SpelFieldDto;
import com.veritas.backend.workflow.dto.WorkflowDto;
import com.veritas.backend.workflow.dto.WorkflowEditDto;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;

import jakarta.persistence.EntityNotFoundException;

import com.veritas.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import org.springframework.security.access.AccessDeniedException;

public interface WorkflowService {

    /**
     * Creates a new workflow definition by parsing and validating the provided BPMN XML.
     * Steps, transitions, and transition rules are extracted and persisted from the BPMN model.
     * An optional department scope can be set to restrict workflow visibility.
     *
     * @param workflowSaveDto the creation payload containing the BPMN XML and optional department ID
     * @return the created {@link WorkflowDto}
     * @throws IllegalArgumentException if the BPMN XML is invalid or fails structural/semantic validation
     * @throws EntityNotFoundException if the specified department is not found
     */
    public WorkflowDto createWorkflow(WorkflowSaveDto workflowSaveDto);

    /**
     * Returns a single workflow definition by ID. Requesters and procurement officers
     * can only access global workflows or those scoped to their department.
     *
     * @param id the workflow ID
     * @param user the authenticated user determining visibility scope
     * @return the matching {@link WorkflowDto}
     * @throws EntityNotFoundException if no workflow exists with the given ID
     * @throws AccessDeniedException if the user is not authorized to access this workflow
     */
    public WorkflowDto getWorkflow(Long id, User user);

    /**
     * Replaces an existing workflow definition with a new version parsed from BPMN XML.
     * The previous version is deactivated and linked as the predecessor.
     * Only active workflows can be edited.
     *
     * @param id the ID of the workflow to update
     * @param workflowEditDto the edit payload containing the updated BPMN XML and optional department ID
     * @return the updated {@link WorkflowDto}
     * @throws EntityNotFoundException if no workflow exists with the given ID
     * @throws IllegalArgumentException if the workflow is inactive, or the BPMN XML is invalid
     */
    public WorkflowDto editWorkflow(Long id, WorkflowEditDto workflowEditDto);

    /**
     * Returns a paginated, filtered list of workflow definitions visible to the authenticated user.
     * Requesters and procurement officers see only global workflows and those scoped to their department.
     * All other roles see everything.
     *
     * @param pageable pagination and sorting parameters
     * @param filter optional search string to filter by workflow name
     * @param isActive optional filter for active or inactive workflows
     * @param authUser the authenticated user determining visibility scope
     * @return a page of matching {@link WorkflowDto}
     */
    public Page<WorkflowDto> getAllWorkflows(Pageable pageable, String filter, Boolean isActive, User authUser);

    /**
     * Soft-deletes a workflow definition by marking it as inactive.
     *
     * @param id the ID of the workflow to delete
     * @throws EntityNotFoundException if no workflow exists with the given ID
     */
    void deleteWorkflow(Long id);

    /**
     * Returns the list of allowed SpEL fields for use in routing expressions and transition rules.
     * Each field includes its fully-qualified path (e.g. {@code department.budget.remainingAmount}),
     * its data type ({@code number}, {@code string}, {@code boolean}, {@code object}), and whether
     * the field represents a nested object that can be further drilled into.
     *
     * @return the list of allowed SpEL fields
     */
    List<SpelFieldDto> getSpelFields();
}
