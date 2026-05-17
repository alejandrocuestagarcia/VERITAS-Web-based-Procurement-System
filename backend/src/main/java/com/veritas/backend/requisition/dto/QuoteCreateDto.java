package com.veritas.backend.requisition.dto;

import java.math.BigDecimal;
import java.util.List;
import com.veritas.backend.vendor.entity.Currency;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record QuoteCreateDto(
    @NotNull(message = "Vendor ID is required")
    Long vendorId,
    
    @NotNull(message = "Currency is required")
    Currency currency,
    
    @NotNull(message = "Base amount is required")
    BigDecimal baseAmount,
    
    @NotNull(message = "Shipping costs are required")
    BigDecimal shippingCosts,
    
    @NotNull(message = "Total amount is required")
    BigDecimal totalAmount,
    
    @Valid
    List<QuoteLineItemCreateDto> items
) {}
