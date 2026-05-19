package com.veritas.backend.requisition.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuoteLineItemCreateDto(
    @NotBlank(message = "Product description is required")
    String productDescription,
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    Integer quantity,
    
    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be greater than 0")
    BigDecimal unitPrice,
    
    Long requestItemId
) {}
