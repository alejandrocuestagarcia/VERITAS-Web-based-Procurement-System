package com.veritas.backend.integrations.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraFieldsRecord(
    String summary,
    JsonNode description,
    JiraPriorityRecord priority,
    String created,
    String updated,
    JiraUserRecord reporter,
    JiraProjectRecord project
) {
}
