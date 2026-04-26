package com.veritas.backend.integrations.jira.mapper;

import com.veritas.backend.integrations.jira.dto.JiraIssueRecord;
import com.veritas.backend.requisition.entity.Request;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JiraIssueMapper {

    @Mapping(target = "jiraIssueKey", source = "key")
    @Mapping(target = "jiraIssueUrl", source = "self")
    @Mapping(target = "requestName", source = "fields.summary")
    @Mapping(target = "jiraStatus", constant = "PENDING_SYNC")
    @Mapping(target = "requestID", ignore = true)
    Request toRequest(JiraIssueRecord issueRecord);
}
