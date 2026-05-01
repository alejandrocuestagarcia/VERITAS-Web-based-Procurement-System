package com.veritas.backend.requisition.dto;

import com.veritas.backend.requisition.entity.Priority;
import lombok.Data;

@Data
public class RequisitionCreateDto {
    private String requestName;
    private String description;
    private Long projectId;
    private Long workflowDefinitionId;
    private Priority priority;
}
