package com.veritas.backend.integrations.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraAttachmentRecord(
    String id,
    String filename,
    String content,
    Long size,
    String mimeType
) {}
