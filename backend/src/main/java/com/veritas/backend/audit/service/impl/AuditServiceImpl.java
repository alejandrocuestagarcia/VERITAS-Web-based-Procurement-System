package com.veritas.backend.audit.service.impl;

import com.veritas.backend.audit.entity.AuditLog;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.audit.service.AuditService;
import com.veritas.backend.user.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void createPasswordResetLog(User actor, String action, String details) {
        String mockHash = java.util.UUID.randomUUID().toString();

        AuditLog log = AuditLog.builder().actor(actor)
                .action(action + ": " + details)
                .entryHash(mockHash)
                .timestamp(java.time.LocalDateTime.now()).build();

        auditLogRepository.save(log);
    }
}