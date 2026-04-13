package com.veritas.backend.workflow.dto;

import lombok.Data;

@Data
public class WorkflowSaveDto {
    private String name;
    private String bpmnXml;
}
