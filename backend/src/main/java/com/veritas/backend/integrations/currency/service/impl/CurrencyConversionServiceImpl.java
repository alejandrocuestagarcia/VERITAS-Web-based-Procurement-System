package com.veritas.backend.integrations.currency.service.impl;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veritas.backend.integrations.currency.dto.CurrencyConversionResponseDto;
import com.veritas.backend.integrations.currency.entity.ExchangeRate;
import com.veritas.backend.integrations.currency.repository.ExchangeRateRepository;
import com.veritas.backend.integrations.currency.service.CurrencyConversionService;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    public CurrencyConversionResponseDto convert(BigDecimal amount, String sourceCurrency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be provided and cannot be negative");
        }

        if (sourceCurrency == null || sourceCurrency.isBlank()) {
            throw new IllegalArgumentException("Source currency is required");
        }

        if ("EUR".equals(sourceCurrency)) {
            return new CurrencyConversionResponseDto(amount, sourceCurrency, amount, BigDecimal.ONE, LocalDateTime.now());
        }

        ExchangeRate exchangeRate = exchangeRateRepository.findTopByTargetCurrencyOrderByFetchedAtDesc(sourceCurrency)
            .orElseThrow(() -> new EntityNotFoundException("No exchange rate available for " + sourceCurrency + " -> EUR"));
        BigDecimal converted = amount.divide(exchangeRate.getRate(), 2, RoundingMode.HALF_UP);

        return new CurrencyConversionResponseDto(amount, sourceCurrency, converted, exchangeRate.getRate(), exchangeRate.getFetchedAt());
    }

}