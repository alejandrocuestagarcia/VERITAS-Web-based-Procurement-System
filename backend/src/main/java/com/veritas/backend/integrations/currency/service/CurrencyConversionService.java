package com.veritas.backend.integrations.currency.service;

import java.math.BigDecimal;

import com.veritas.backend.integrations.currency.dto.CurrencyConversionResult;
import com.veritas.backend.integrations.currency.entity.Currency;

public interface CurrencyConversionService {

    /**
     * Converts the given amount from the source currency to EUR using the latest available exchange rate.
     * If the source currency is already EUR, the amount is returned as-is with a rate of 1.
     *
     * @param amount the amount to convert, must be non-null and non-negative
     * @param sourceCurrency the currency to convert from, must be non-null
     * @return a {@link CurrencyConversionResult} containing the converted amount, rate used and rate metadata
     * @throws IllegalArgumentException if the amount is null or negative, or the source currency is null
     * @throws EntityNotFoundException if no exchange rate is available for the given currency
     */
    CurrencyConversionResult convert(BigDecimal amount, Currency sourceCurrency);
}