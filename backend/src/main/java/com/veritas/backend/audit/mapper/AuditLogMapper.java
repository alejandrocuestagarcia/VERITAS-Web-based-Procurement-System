package com.veritas.backend.audit.mapper;

import com.veritas.backend.audit.dto.AuditLogDto;
import com.veritas.backend.audit.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "requestName", expression = "java(log.getRequest() != null ? log.getRequest().getRequestName() : null)")
    @Mapping(target = "requestKey", expression = "java(log.getRequest() != null ? log.getRequest().getJiraIssueKey() : null)")
    @Mapping(target = "user", expression = "java(log.getActor() != null ? log.getActor().getEmail() : \"System\")")
    @Mapping(target = "currentHash", source = "entryHash")
    AuditLogDto jiraSyncLogtoDto(AuditLog log);
}
