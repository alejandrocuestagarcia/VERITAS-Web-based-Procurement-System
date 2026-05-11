package com.veritas.backend.requisition.dto;

import com.veritas.backend.requisition.entity.Priority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RequisitionCreateDto(
    @NotBlank(message = "Request name is required")
    String requestName,

    String description,

    @NotNull(message = "Project is required")
    Long projectId,

    @NotNull(message = "Workflow is required")
    Long workflowDefinitionId,

    @NotNull(message = "Priority is required")
    Priority priority,

    @NotEmpty(message = "At least one item is required")
    @Valid
    List<RequisitionItemCreateDto> items
) {}
