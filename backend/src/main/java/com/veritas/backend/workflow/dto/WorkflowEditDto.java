package com.veritas.backend.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkflowEditDto(
        @NotBlank String bpmnXml,
        Long departmentId
        ){
}
