package com.veritas.backend.project.mapper;

import com.veritas.backend.project.dto.ProjectCreationDto;
import com.veritas.backend.project.dto.ProjectDto;
import com.veritas.backend.project.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface ProjectMapper {
    @Mapping(source = "team.name", target = "teamName")
    ProjectDto toProjectDto(Project project);

    Project toProject(ProjectCreationDto dto);
}
