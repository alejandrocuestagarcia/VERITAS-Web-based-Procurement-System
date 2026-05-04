package com.veritas.backend.requisition.dto;

import com.veritas.backend.requisition.entity.Priority;
import java.util.List;

public record RequisitionCreateDto(
    String requestName,
    String description,
    Long projectId,
    Long workflowDefinitionId,
    Priority priority,
    List<RequisitionItemCreateDto> items
) {}
