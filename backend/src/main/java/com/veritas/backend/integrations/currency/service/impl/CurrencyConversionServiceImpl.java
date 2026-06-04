package com.veritas.backend.integrations.currency.service.impl;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
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
    public CurrencyConversionResponseDto convert(BigDecimal amount, String targetCurrency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be provided and cannot be negative");
        }

        if (targetCurrency == null || targetCurrency.isBlank()) {
            throw new IllegalArgumentException("Target currency is required");
        }

        if ("EUR".equals(targetCurrency)) {
            return new CurrencyConversionResponseDto(amount, targetCurrency, amount, BigDecimal.ONE, LocalDateTime.now());
        }

        ExchangeRate exchangeRate = exchangeRateRepository.findTopByTargetCurrencyOrderByFetchedAtDesc(targetCurrency)
            .orElseThrow(() -> new EntityNotFoundException("No exchange rate available for EUR -> " + targetCurrency));
        BigDecimal converted = amount.multiply(exchangeRate.getRate());

        return new CurrencyConversionResponseDto(amount, targetCurrency, converted, exchangeRate.getRate(), exchangeRate.getFetchedAt());
    }

}