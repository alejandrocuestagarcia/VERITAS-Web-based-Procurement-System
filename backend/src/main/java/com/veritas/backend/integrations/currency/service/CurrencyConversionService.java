package com.veritas.backend.integrations.currency.service;

import java.math.BigDecimal;

import com.veritas.backend.integrations.currency.entity.Currency;

public interface CurrencyConversionService {
    BigDecimal convert(BigDecimal amount, Currency sourceCurrency);
}