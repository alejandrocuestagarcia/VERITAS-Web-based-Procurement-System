package com.veritas.backend.integrations.jira.dto;

public record JiraIssueRecord(String id, String key, String self, JiraFieldsRecord fields) {
}
