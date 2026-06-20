package com.veritas.backend.integrations.jira.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record JiraConfigDto(
    Long id,

    @NotBlank(message = "Config name is required")
    @Size(max = 120, message = "Config name must be at most 120 characters")
    String name,

    @NotBlank(message = "Jira URL is required")
    @Pattern(regexp = "^https?://.*", message = "Jira URL must start with http:// or https://")
    @Size(max = 255, message = "Jira URL must be at most 255 characters")
    String jiraUrl,

    @NotBlank(message = "Username/Email is required")
    @Size(max = 120, message = "Username must be at most 120 characters")
    String username,

    @Size(max = 255, message = "API Token must be at most 255 characters")
    String apiToken,

    @NotBlank(message = "JQL query is required")
    @Size(max = 1000, message = "JQL query must be at most 1000 characters")
    String jql,

    @NotNull(message = "Sync interval is required")
    @Min(value = 1, message = "Sync interval must be at least 1 minute")
    Integer syncIntervalMinutes,

    @NotBlank(message = "Custom Field ID is required")
    @Size(max = 120, message = "Custom Field ID must be at most 120 characters")
    String customFieldId,

    @NotNull(message = "Fallback user is required")
    Long fallbackUserId,

    @NotNull(message = "Fallback project is required")
    Long fallbackProjectId,

    @NotNull(message = "Used workflow is required")
    Long fallbackWorkflowId,

    LocalDateTime lastSyncTime,
    LocalDateTime nextSyncTime
) {
}
