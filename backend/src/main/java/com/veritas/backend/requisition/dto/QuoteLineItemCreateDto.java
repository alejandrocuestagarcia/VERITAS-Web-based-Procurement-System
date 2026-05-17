package com.veritas.backend.requisition.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuoteLineItemCreateDto(
    @NotBlank(message = "Product description is required")
    String productDescription,
    
    @NotNull(message = "Quantity is required")
    Integer quantity,
    
    @NotNull(message = "Unit price is required")
    BigDecimal unitPrice,
    
    Long requestItemId
) {}
