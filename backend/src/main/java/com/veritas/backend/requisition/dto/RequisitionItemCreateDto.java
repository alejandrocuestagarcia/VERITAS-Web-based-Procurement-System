package com.veritas.backend.requisition.dto;

import com.veritas.backend.requisition.entity.RequestItemUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RequisitionItemCreateDto(
    @NotBlank(message = "Item name is required")
    @Size(max = 120, message = "Item name must be at most 120 characters")
    String name,

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    Integer quantity,

    @NotNull(message = "Unit is required")
    RequestItemUnit unit,

    @Size(max = 3000, message = "Description must be at most 3000 characters")
    String description
) {}
