package com.veritas.backend.workflow.service;

import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.common.exception.WorkflowStateException;
import com.veritas.backend.requisition.entity.Request;

public interface WorkflowEngineService {

    /**
     * Advances a request to the next eligible workflow step based on transition conditions.
     * Handles automated approval steps and branch gateways recursively.
     * Validates transition rules (required vendors, attachments, advanced SpEL rules) and
     * budget safety buffer before committing the transition. Enqueues a Jira sync on completion.
     *
     * @param requisition the request to advance
     * @param actor the user performing the approval
     * @param nextAssigneeId the ID of the user to assign at the next step, if applicable
     * @throws WorkflowStateException if no valid transition exists,
     *         a transition rule is violated, the budget safety buffer is exceeded,
     *         or the gateway recursion limit is reached
     * @throws AccessDeniedException if the actor is not authorized for the current step
     */
    public void moveToNextStep(Request requisition, User actor, Long nextAssigneeId);

    /**
     * Reverts a request to the step it was at before the current one, resolved from audit history.
     * Branch and gateway steps are skipped during history traversal.
     * If the target step is the START_EVENT, the request is returned to DRAFT.
     * Enqueues a Jira sync on completion.
     *
     * @param request the request to revert
     * @param actor the user performing the revert
     * @param reason the reason for reverting, included in the audit log
     * @throws WorkflowStateException if no valid previous step can be resolved
     * @throws AccessDeniedException if the actor is not authorized for the current step
     */
    public void revertToPreviousStep(Request request, User actor, String reason);

    /**
     * Starts a request's workflow by logging the submission at the START_EVENT step
     * and immediately advancing to the first actionable step.
     *
     * @param request the request to start
     * @param actor the user submitting the request
     * @param nextAssigneeId the ID of the initial assignee, if applicable
     * @throws WorkflowStateException if the workflow has no START_EVENT step defined
     */
    void startWorkflow(Request request, User actor, Long nextAssigneeId);

    /**
     * Resolves the next workflow step for a request without advancing it.
     * For DRAFT requests, resolution starts from the START_EVENT step.
     * Branch gateways are traversed transparently.
     *
     * @param request the request to inspect
     * @return the next {@link WorkflowStep}, or {@code null} if no eligible transition exists
     */
    WorkflowStep getNextStep(Request request);

    /**
     * Checks whether the given actor is authorized to act on the specified workflow step.
     * Automated approval steps bypass all authorization checks.
     * Role, team leader, department, and assignee constraints are all enforced.
     *
     * @param request the request being acted upon
     * @param actor the user attempting to act
     * @param currentStep the step to authorize against
     * @throws AccessDeniedException if the actor is not authorized
     */
    void checkAuthorization(Request request, User actor, WorkflowStep currentStep);
}
