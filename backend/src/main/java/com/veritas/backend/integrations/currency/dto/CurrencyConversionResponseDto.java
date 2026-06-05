package com.veritas.backend.integrations.currency.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CurrencyConversionResponseDto (
    BigDecimal amount,
    String sourceCurrency,
    BigDecimal convertedAmount,
    BigDecimal appliedRate,
    LocalDateTime rateFetchedAt
) {}