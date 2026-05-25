package com.veritas.backend.integrations.jira.mapper;

import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import com.veritas.backend.integrations.jira.dto.JiraConfigResponseDto;
import com.veritas.backend.integrations.jira.entity.JiraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface JiraConfigMapper {
    @Mapping(target = "isTokenSet", expression = "java(entity.getApiToken() != null && !entity.getApiToken().isBlank())")
    @Mapping(target = "fallbackUserId", source = "fallbackUser.id")
    @Mapping(target = "fallbackUserName", source = "fallbackUser.name")
    @Mapping(target = "fallbackProjectId", source = "fallbackProject.id")
    @Mapping(target = "fallbackProjectName", source = "fallbackProject.name")
    @Mapping(target = "fallbackWorkflowId", source = "fallbackWorkflow.id")
    @Mapping(target = "fallbackWorkflowName", source = "fallbackWorkflow.name")
    JiraConfigResponseDto toDto(JiraConfig entity);

    @Mapping(target = "fallbackUser", ignore = true)
    @Mapping(target = "fallbackProject", ignore = true)
    @Mapping(target = "fallbackWorkflow", ignore = true)
    JiraConfig toEntity(JiraConfigDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastSyncTime", ignore = true)
    @Mapping(target = "fallbackUser", ignore = true)
    @Mapping(target = "fallbackProject", ignore = true)
    @Mapping(target = "fallbackWorkflow", ignore = true)
    void updateEntityFromDto(JiraConfigDto dto, @MappingTarget JiraConfig entity);
}
