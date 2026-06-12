package com.veritas.backend.integrations.currency.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.veritas.backend.integrations.currency.entity.ExchangeRateSource;

public record CurrencyConversionResult(
    BigDecimal convertedAmount,
    BigDecimal exchangeRate,
    LocalDateTime fetchedAt,
    ExchangeRateSource source
) {}
