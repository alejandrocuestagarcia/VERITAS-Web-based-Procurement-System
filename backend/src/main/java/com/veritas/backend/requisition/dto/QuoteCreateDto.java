package com.veritas.backend.requisition.dto;

import java.math.BigDecimal;
import java.util.List;

import com.veritas.backend.integrations.currency.entity.Currency;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.Builder;

@Builder
public record QuoteCreateDto(
    @NotNull(message = "Vendor ID is required")
    Long vendorId,
    
    @NotNull(message = "Currency is required")
    Currency currency,
    
    @NotNull(message = "Base amount is required")
    @Positive(message = "Base amount must be greater than 0")
    BigDecimal baseAmount,
    
    @NotNull(message = "Shipping costs are required")
    @PositiveOrZero(message = "Shipping costs must be greater than or equal to 0")
    BigDecimal shippingCosts,
    
    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be greater than 0")
    BigDecimal totalAmount,

    @NotNull(message = "Shipping time is required")
    @PositiveOrZero(message = "Shipping time must be greater than or equal to 0")
    Integer shippingTime,
    
    @Valid
    List<QuoteLineItemCreateDto> items
) {}
