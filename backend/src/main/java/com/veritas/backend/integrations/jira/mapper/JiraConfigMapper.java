package com.veritas.backend.integrations.jira.mapper;

import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import com.veritas.backend.integrations.jira.entity.JiraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface JiraConfigMapper {
    JiraConfigDto toDto(JiraConfig entity);

    JiraConfig toEntity(JiraConfigDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastSyncTime", ignore = true)
    void updateEntityFromDto(JiraConfigDto dto, @MappingTarget JiraConfig entity);
}
