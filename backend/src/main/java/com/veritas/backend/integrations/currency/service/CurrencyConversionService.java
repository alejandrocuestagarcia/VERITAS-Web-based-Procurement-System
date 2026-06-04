package com.veritas.backend.integrations.currency.service;

import java.math.BigDecimal;

import com.veritas.backend.integrations.currency.dto.CurrencyConversionResponseDto;

public interface CurrencyConversionService {
    CurrencyConversionResponseDto convert(BigDecimal amount, String toCurrency);
}