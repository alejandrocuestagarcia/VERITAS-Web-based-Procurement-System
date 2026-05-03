package com.veritas.backend.audit.dto;

import java.time.LocalDateTime;

public record AuditLogDto(
        String requestName,
        String requestKey,
        String jiraIssueUrl,
        String user,
        LocalDateTime timestamp,
        String action,
        String previousStatus,
        String newStatus,
        String currentHash,
        String previousHash
) {}