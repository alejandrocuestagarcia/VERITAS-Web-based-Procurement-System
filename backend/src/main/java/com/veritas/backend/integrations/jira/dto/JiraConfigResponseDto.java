package com.veritas.backend.integrations.jira.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record JiraConfigResponseDto(
    @NotNull
    Long id,
    @NotBlank
    String name,
    @NotBlank
    String jiraUrl,
    @NotBlank
    String username,
    @NotBlank
    String jql,
    @NotNull
    Integer syncIntervalMinutes,
    @NotBlank
    String customFieldId,
    LocalDateTime lastSyncTime,
    LocalDateTime nextSyncTime,
    boolean isTokenSet
) {
}
