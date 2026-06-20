package com.veritas.backend.requisition.dto;

import com.veritas.backend.requisition.entity.Priority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RequisitionCreateDto(
    @NotBlank(message = "Request name is required")
    @Size(max = 120, message = "Request name must be at most 120 characters")
    String requestName,

    @Size(max = 3000, message = "Description must be at most 3000 characters")
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
