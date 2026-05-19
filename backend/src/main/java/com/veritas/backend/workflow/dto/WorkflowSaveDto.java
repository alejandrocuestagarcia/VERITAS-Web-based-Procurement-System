package com.veritas.backend.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkflowSaveDto (
    @NotBlank String bpmnXml,
    Long departmentId
){}
