package com.veritas.backend.requisition.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.integrations.currency.entity.ExchangeRateSource;

@Builder
public record InvoiceDto(
    Long invoiceId,
    Long requestId,
    String invoiceNumber,
    LocalDate invoiceDate,
    BigDecimal totalAmount,
    Currency currency,
    BigDecimal totalAmountEuro,
    LocalDateTime exchangeRateFetchedAt,
    ExchangeRateSource exchangeRateSource,
    LocalDate dueDate,
    Boolean isPaid,
    Long vendorId,
    String vendorName,
    LocalDateTime createdAt
) {}
