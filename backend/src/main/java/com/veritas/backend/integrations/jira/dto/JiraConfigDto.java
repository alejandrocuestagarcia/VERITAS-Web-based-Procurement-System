package com.veritas.backend.integrations.jira.dto;

import java.time.LocalDateTime;

public record JiraConfigDto(
    Long id,
    String name,
    String jiraUrl,
    String username,
    String apiToken,
    String jql,
    Integer syncIntervalMinutes,
    String customFieldId,
    LocalDateTime lastSyncTime,
    LocalDateTime nextSyncTime
) {
}
