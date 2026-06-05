package com.veritas.backend.audit.service.impl;

import com.veritas.backend.audit.dto.AuditLogDto;
import com.veritas.backend.audit.entity.AuditLog;
import com.veritas.backend.audit.mapper.AuditLogMapper;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.audit.service.AuditService;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.veritas.backend.workflow.entity.WorkflowTransition;
import com.veritas.backend.workflow.entity.WorkflowStep;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.veritas.backend.common.model.AuditActionConstants.*;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
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

    @Override
    public void createJiraUnsyncLog(User actor, Request request, String details) {
        String mockHash = UUID.randomUUID().toString();

        AuditLog log = AuditLog.builder()
                .request(request)
                .actor(actor)
                .action(JIRA_UNSYNC)
                .description(details)
                .entryHash(mockHash)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    @Override
    public void createJiraRequestUpdatedLog(User actor, Request request, String details) {
        String mockHash = UUID.randomUUID().toString();

        AuditLog log = AuditLog.builder()
                .request(request)
                .actor(actor)
                .action(JIRA_REQUEST_UPDATED)
                .description(details)
                .entryHash(mockHash)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    @Override
    public Page<AuditLogDto> getJiraIssueLogsByAction(String action, Pageable pageable, String search) {
        return auditLogRepository.findAllByAction(action, pageable, search)
                .map(auditLogMapper::jiraSyncLogtoDto);
    }

    @Override
    public Page<AuditLogDto> getJiraIssueLogsByActions(java.util.List<String> actions, Pageable pageable, String search) {
        return auditLogRepository.findAllByActionIn(actions, pageable, search)
                .map(auditLogMapper::jiraSyncLogtoDto);
    }

    @Override
    public void createWorkflowTransitionLog(User actor, Request request, WorkflowTransition transition, String action, String description) {
        String mockHash = UUID.randomUUID().toString();

        WorkflowStep fromStep = (transition != null) ? transition.getFromStep() : null;
        WorkflowStep toStep = (transition != null) ? transition.getToStep() : request.getCurrentStep();

        AuditLog log = AuditLog.builder()
                .request(request)
                .actor(actor)
                .action(action)
                .description(description)
                .previousStep(fromStep)
                .newStep(toStep)
                .transition(transition)
                .entryHash(mockHash)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    @Override
    public void createRequisitionChangeLog(User actor, Request request, String details) {
        String mockHash = UUID.randomUUID().toString();

        AuditLog log = AuditLog.builder()
                .request(request)
                .actor(actor)
                .action(REQUISITION_EDITED)
                .description(details)
                .entryHash(mockHash)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    @Override
    public List<AuditLogDto> getAuditLogsByRequestId(Long requestId) {
        return auditLogRepository.findAllByRequestIdOrderByTimestampAsc(requestId)
                .stream()
                .map(auditLogMapper::toDto)
                .collect(Collectors.toList());
    }
}