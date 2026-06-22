package com.veritas.backend.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkflowEditDto(
        @NotBlank
        @Size(max = 100000, message = "BPMN XML must be at most 100,000 characters")
        String bpmnXml,
        Long departmentId
        ){
}
