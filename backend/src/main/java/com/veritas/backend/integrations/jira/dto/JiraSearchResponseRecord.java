package com.veritas.backend.integrations.jira.dto;

import java.util.List;

public record JiraSearchResponseRecord(List<JiraIssueRecord> issues) {
}
