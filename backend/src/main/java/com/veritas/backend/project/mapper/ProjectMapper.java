package com.veritas.backend.project.mapper;

import com.veritas.backend.project.dto.ProjectCreationDto;
import com.veritas.backend.project.dto.ProjectDto;
import com.veritas.backend.project.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface ProjectMapper {
    @Mapping(source = "team.name", target = "teamName")
    @Mapping(source = "internalBudget.totalAmount", target = "budget")
    @Mapping(source = "internalBudget.committedSpend", target = "committedSpend")
    @Mapping(source = "internalBudget.actualSpend", target = "actualSpend")
    @Mapping(source = "internalBudget.safetyBuffer", target = "safetyBuffer")
    ProjectDto toProjectDto(Project project);

    @Mapping(target = "internalBudget", ignore = true) // Handled in Service
    Project toProject(ProjectCreationDto dto);
}
