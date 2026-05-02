package com.veritas.backend.workflow.dto;

public record WorkflowDto (
    Long id,
    String name,
    String bpmnXml,
    Long version,
    String description,
    Boolean isActive
){}
