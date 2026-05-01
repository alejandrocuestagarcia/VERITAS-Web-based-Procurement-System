package com.veritas.backend.audit.service.impl;

import com.veritas.backend.audit.dto.AuditLogDto;
import com.veritas.backend.audit.entity.AuditLog;
import com.veritas.backend.audit.mapper.AuditLogMapper;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.audit.service.AuditService;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.user.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.veritas.backend.common.model.AuditActionConstants.JIRA_SYNC;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional
    public void createPasswordResetLog(User actor, String action, String details) {
        String mockHash = UUID.randomUUID().toString();

        AuditLog log = AuditLog.builder().actor(actor)
                .action(action)
                .description(details)
                .entryHash(mockHash)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    @Override
    @Transactional
    public void createJiraSyncLog(User actor, Request request, String details) {
        String mockHash = UUID.randomUUID().toString();

        AuditLog log = AuditLog.builder()
                .request(request)
                .actor(actor)
                .action(JIRA_SYNC)
                .description(details)
                .entryHash(mockHash)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    public List<AuditLogDto> getJiraIssueLogsByAction(String action) {
        return auditLogRepository.findAllByAction(action).stream()
                .map(auditLogMapper::jiraSyncLogtoDto)
                .toList();
    }
}