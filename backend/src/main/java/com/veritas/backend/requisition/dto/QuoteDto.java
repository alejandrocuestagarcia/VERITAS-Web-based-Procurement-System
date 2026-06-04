package com.veritas.backend.requisition.dto;

import java.math.BigDecimal;
import java.util.List;

import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.vendor.dto.VendorDto;

public record QuoteDto(
    Long quoteId,
    Long vendorId,
    VendorDto vendor,
    Currency currency,
    BigDecimal baseAmount,
    BigDecimal shippingCosts,
    BigDecimal totalAmount,
    Boolean isSelected,
    List<QuoteLineItemDto> items
) {}
