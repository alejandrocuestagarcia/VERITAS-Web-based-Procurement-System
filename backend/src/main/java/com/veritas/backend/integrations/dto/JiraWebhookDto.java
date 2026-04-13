package com.veritas.backend.integrations.dto;

import lombok.Data;

@Data
public class JiraWebhookDto {
    private String issueId;
    private String issueKey;
    private String eventType;
}
