package com.veritas.backend.workflow.mapper;

import com.veritas.backend.workflow.dto.WorkflowDto;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import org.mapstruct.Mapper;

@Mapper(unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface WorkflowMapper {

    WorkflowDefinition toWorkflowDefinition(WorkflowSaveDto workflowSaveDto);

    WorkflowDto toWorkflowDto(WorkflowDefinition workflowDefinition);
}
