package com.veritas.backend.requisition.dto;

import com.veritas.backend.requisition.entity.RequestItemUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RequisitionItemCreateDto(
    @NotBlank(message = "Item name is required")
    String name,

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    Integer quantity,

    @NotNull(message = "Unit is required")
    RequestItemUnit unit,

    String description
) {}
