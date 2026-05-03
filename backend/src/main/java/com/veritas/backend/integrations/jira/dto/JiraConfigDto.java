package com.veritas.backend.integrations.jira.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

public record JiraConfigDto(
    Long id,

    @NotBlank(message = "Config name is required")
    String name,

    @NotBlank(message = "Jira URL is required")
    @Pattern(regexp = "^https?://.*", message = "Jira URL must start with http:// or https://")
    String jiraUrl,

    @NotBlank(message = "Username/Email is required")
    String username,

    String apiToken,

    @NotBlank(message = "JQL query is required")
    String jql,

    @NotNull(message = "Sync interval is required")
    @Min(value = 1, message = "Sync interval must be at least 1 minute")
    Integer syncIntervalMinutes,

    @NotBlank(message = "Custom Field ID is required")
    String customFieldId,

    LocalDateTime lastSyncTime,
    LocalDateTime nextSyncTime
) {
}
