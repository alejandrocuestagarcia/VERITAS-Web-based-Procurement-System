package com.veritas.backend.requisition.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.integrations.currency.entity.ExchangeRateSource;
import com.veritas.backend.vendor.dto.VendorDto;

public record QuoteDto(
    Long quoteId,
    Long vendorId,
    VendorDto vendor,
    Currency currency,
    BigDecimal baseAmount,
    BigDecimal shippingCosts,
    BigDecimal totalAmount,
    BigDecimal totalAmountEuro,
    LocalDateTime exchangeRateFetchedAt,
    ExchangeRateSource exchangeRateSource,
    Boolean isSelected,
    List<QuoteLineItemDto> items
) {}
