package com.veritas.backend.audit.mapper;

import com.veritas.backend.audit.dto.AuditLogDto;
import com.veritas.backend.audit.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "requestName", expression = "java(log.getRequest() != null ? log.getRequest().getRequestName() : null)")
    @Mapping(target = "requestKey", expression = "java(log.getRequest() != null ? log.getRequest().getJiraIssueKey() : null)")
    @Mapping(target = "jiraIssueUrl", expression = "java(log.getRequest() != null ? log.getRequest().getJiraIssueUrl() : null)")
    @Mapping(target = "user", expression = "java(log.getActor() != null ? log.getActor().getEmail() : \"System\")")
    @Mapping(target = "currentHash", source = "entryHash")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "previousStatus", ignore = true)
    @Mapping(target = "newStatus", ignore = true)
    AuditLogDto jiraSyncLogtoDto(AuditLog log);

    @Mapping(target = "requestName", expression = "java(log.getRequest() != null ? log.getRequest().getRequestName() : null)")
    @Mapping(target = "requestKey", expression = "java(log.getRequest() != null ? log.getRequest().getRequestKey() : null)")
    @Mapping(target = "jiraIssueUrl", expression = "java(log.getRequest() != null ? log.getRequest().getJiraIssueUrl() : null)")
    @Mapping(target = "user", expression = "java(log.getActor() != null ? log.getActor().getEmail() : \"System\")")
    @Mapping(target = "previousStatus", expression = "java(log.getPreviousStep() != null ? log.getPreviousStep().getName() : null)")
    @Mapping(target = "newStatus", expression = "java(log.getNewStep() != null ? log.getNewStep().getName() : null)")
    @Mapping(target = "currentHash", source = "entryHash")
    AuditLogDto toDto(AuditLog log);
}
