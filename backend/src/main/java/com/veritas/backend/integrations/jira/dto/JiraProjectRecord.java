package com.veritas.backend.integrations.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraProjectRecord(String key, String name) {
}
