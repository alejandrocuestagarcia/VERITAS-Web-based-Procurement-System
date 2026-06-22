package com.veritas.backend.requisition.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record QuoteLineItemCreateDto(
    @NotBlank(message = "Product description is required")
    @Size(max = 255, message = "Product description must be at most 255 characters")
    String productDescription,
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    Integer quantity,
    
    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be greater than 0")
    BigDecimal unitPrice,
    
    Long requestItemId
) {}
