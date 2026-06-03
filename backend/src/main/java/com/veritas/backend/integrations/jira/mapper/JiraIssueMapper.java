package com.veritas.backend.integrations.jira.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.veritas.backend.integrations.jira.dto.JiraIssueRecord;
import com.veritas.backend.requisition.entity.Priority;
import com.veritas.backend.requisition.entity.Request;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface JiraIssueMapper {

    @Mapping(target = "jiraIssueKey", source = "key")
    @Mapping(target = "jiraIssueUrl", source = "self")
    @Mapping(target = "requestName", source = "fields.summary")
    @Mapping(target = "description", source = "fields.description", qualifiedByName = "mapDescription")
    @Mapping(target = "priority", source = "fields.priority.name", qualifiedByName = "mapPriority")
    @Mapping(target = "jiraStatus", constant = "PENDING_SYNC")
    @Mapping(target = "requestID", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "budget", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "workflowDefinition", ignore = true)
    @Mapping(target = "currentStep", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "requestKey", ignore = true)
    @Mapping(target = "totalQuantity", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Request toRequest(JiraIssueRecord issueRecord);

    @Named("mapDescription")
    default String mapDescription(JsonNode description) {
        if (description == null) {
            return null;
        }
        return description.toString();
    }

    @Named("mapPriority")
    default Priority mapPriority(String jiraPriority) {
        if (jiraPriority == null)
            return Priority.MEDIUM;
        return switch (jiraPriority.toUpperCase()) {
            case "HIGHEST", "CRITICAL" -> Priority.CRITICAL;
            case "HIGH" -> Priority.HIGH;
            case "MEDIUM", "MAJOR", "DEFAULT" -> Priority.MEDIUM;
            case "LOW", "MINOR" -> Priority.LOW;
            case "LOWEST" -> Priority.LOW;
            default -> Priority.MEDIUM;
        };
    }
}
