package com.veritas.backend.audit.service;

import com.veritas.backend.audit.dto.AuditLogDto;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.veritas.backend.workflow.entity.WorkflowTransition;

public interface AuditService {
    /**
     * Records a new action. Actor is the User entity performing the action.
     * Required for administrative traceability.
     */
    void createPasswordResetLog(User actor, String action, String details);
    void createJiraSyncLog(User actor, Request request, String details);
    Page<AuditLogDto> getJiraIssueLogsByAction(String action, Pageable pageable, String search);
    void createWorkflowTransitionLog(User actor, Request request, WorkflowTransition transition, String action, String description);
    void createRequisitionChangeLog(User actor, Request request, String details);
}