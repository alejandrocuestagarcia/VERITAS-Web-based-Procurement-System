package com.veritas.backend.integrations.currency.service;

import java.math.BigDecimal;

import com.veritas.backend.integrations.currency.dto.CurrencyConversionResult;
import com.veritas.backend.integrations.currency.entity.Currency;

public interface CurrencyConversionService {
    CurrencyConversionResult convert(BigDecimal amount, Currency sourceCurrency);
}