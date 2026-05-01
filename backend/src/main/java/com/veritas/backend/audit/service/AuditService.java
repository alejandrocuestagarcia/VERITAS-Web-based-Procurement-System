package com.veritas.backend.audit.service;

import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.user.entity.User;

public interface AuditService {
    /**
     * Records a new action. Actor is the User entity performing the action.
     * Required for administrative traceability.
     */
    void createPasswordResetLog(User actor, String action, String details);
    void createJiraSyncLog(User actor, Request request, String action, String details);
}