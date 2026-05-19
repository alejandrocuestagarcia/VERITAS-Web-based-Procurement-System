package com.veritas.backend.workflow.dto;

import com.veritas.backend.department.dto.DepartmentDto;

public record WorkflowDto (
    Long id,
    String name,
    String bpmnXml,
    Long version,
    String description,
    Boolean isActive,
    DepartmentDto department
){}
