package com.veritas.backend.integrations.currency.service.impl;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veritas.backend.integrations.currency.dto.CurrencyConversionResult;
import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.integrations.currency.entity.ExchangeRate;
import com.veritas.backend.integrations.currency.repository.ExchangeRateRepository;
import com.veritas.backend.integrations.currency.service.CurrencyConversionService;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    public CurrencyConversionResult convert(BigDecimal amount, Currency sourceCurrency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be provided and cannot be negative");
        }

        if (sourceCurrency == null) {
            throw new IllegalArgumentException("Source currency is required");
        }

        if (Currency.EUR.equals(sourceCurrency)) {
            return new CurrencyConversionResult(amount, BigDecimal.ONE, null, null);
        }

        ExchangeRate exchangeRate = exchangeRateRepository.findTopByTargetCurrencyOrderByFetchedAtDesc(sourceCurrency)
            .orElseThrow(() -> new EntityNotFoundException("No exchange rate available for " + sourceCurrency + " -> EUR"));

        BigDecimal convertedAmount = amount.divide(exchangeRate.getRate(), 2, RoundingMode.HALF_UP);
        return new CurrencyConversionResult(convertedAmount, exchangeRate.getRate(), exchangeRate.getFetchedAt(), exchangeRate.getSource());
    }

}