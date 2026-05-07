package com.veritas.backend.requisition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RequisitionItemCreateDto(
    @NotBlank(message = "Item name is required")
    String name,

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    Integer quantity,
    BigDecimal estimatedPrice,
    String description
) {}
