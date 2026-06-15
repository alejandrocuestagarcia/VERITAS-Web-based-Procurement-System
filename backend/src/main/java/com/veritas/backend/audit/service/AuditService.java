package com.veritas.backend.audit.service;

import com.veritas.backend.audit.dto.AuditLogDto;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.veritas.backend.workflow.entity.WorkflowTransition;

import java.util.List;

public interface AuditService {
    
    /**
     * Records a password reset action performed by the given actor.
     * Required for administrative traceability.
     *
     * @param actor   the {@link User} performing the password reset action
     * @param action  the action identifier (e.g. "PASSWORD_RESET_REQUESTED")
     * @param details a human-readable description of the event
     */
    void createPasswordResetLog(User actor, String action, String details);

    /**
     * Records an audit entry when a {@link Request} is successfully synced to a Jira issue.
     *
     * @param actor   the {@link User} who triggered the sync
     * @param request the {@link Request} that was synced
     * @param details a human-readable description of the sync event
     */
    void createJiraSyncLog(User actor, Request request, String details);

     /**
     * Records an audit entry when a {@link Request} is unsynced from its associated Jira issue.
     *
     * @param actor   the {@link User} who triggered the unsync
     * @param request the {@link Request} that was unsynced
     * @param details a human-readable description of the unsync event
     */
    void createJiraUnsyncLog(User actor, Request request, String details);

    /**
     * Records an audit entry when a Jira-linked {@link Request} is updated.
     *
     * @param actor   the {@link User} who performed the update
     * @param request the {@link Request} whose changes were pushed to Jira
     * @param details a human-readable description of what was updated
     */
    void createJiraRequestUpdatedLog(User actor, Request request, String details);

    /**
     * Retrieves a paginated list of Jira-related audit logs filtered by a single action type.
     *
     * @param action   the action identifier to filter by (e.g. "JIRA_SYNC")
     * @param pageable pagination parameters
     * @param search   an optional search string to further filter results
     * @return a {@link Page} of {@link AuditLogDto} matching the given action
     */
    Page<AuditLogDto> getJiraIssueLogsByAction(String action, Pageable pageable, String search);

    /**
     * Retrieves a paginated list of Jira-related audit logs filtered by multiple action types.
     *
     * @param actions  a list of action identifiers to include (e.g. ["JIRA_SYNC", "JIRA_UNSYNC"])
     * @param pageable pagination parameters
     * @param search   an optional search string to further filter results
     * @return a {@link Page} of {@link AuditLogDto} matching any of the given actions
     */
    Page<AuditLogDto> getJiraIssueLogsByActions(java.util.List<String> actions, Pageable pageable, String search);

    /**
     * Records an audit entry for a workflow state transition on a {@link Request}.
     * Captures both the originating and target workflow steps derived from the transition.
     * If {@code transition} is {@code null}, the current step of the request is used as the target.
     *
     * @param actor       the {@link User} who triggered the transition
     * @param request     the {@link Request} undergoing the transition
     * @param transition  the {@link WorkflowTransition} that was executed, or {@code null} for initial placement
     * @param action      the action identifier describing the transition event
     * @param description a human-readable description of the transition
     */
    void createWorkflowTransitionLog(User actor, Request request, WorkflowTransition transition, String action, String description);

    /**
     * Records an audit entry when the details of a {@link Request} are edited.
     *
     * @param actor   the {@link User} who made the changes
     * @param request the {@link Request} that was modified
     * @param details a human-readable summary of what was changed
     */
    void createRequisitionChangeLog(User actor, Request request, String details);

    /**
     * Records an audit entry when a notification is sent regarding a {@link Request}.
     *
     * @param actor   the {@link User} associated with the notification
     * @param request the {@link Request} the notification is about
     * @param details a human-readable description of the notification event
     */
    void createNotificationLog(User actor, Request request, String details);

    /**
     * Records an audit entry when a comment is posted to a Jira issue associated with a {@link Request}.
     *
     * @param actor   the {@link User} who posted the comment
     * @param request the {@link Request} associated with the Jira issue
     * @param details a human-readable description of the comment event
     */
    void createJiraCommentLog(User actor, Request request, String details);

    /**
     * Retrieves all audit log entries associated with a given request, ordered by timestamp descending.
     *
     * @param requestId the ID of the {@link Request} whose audit history is being retrieved
     * @return a {@link List} of {@link AuditLogDto} representing the full audit trail for the request
     */
    List<AuditLogDto> getAuditLogsByRequestId(Long requestId);
}