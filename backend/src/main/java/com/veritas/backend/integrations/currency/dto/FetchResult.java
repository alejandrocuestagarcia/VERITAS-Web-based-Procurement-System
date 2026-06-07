package com.veritas.backend.integrations.currency.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.veritas.backend.integrations.currency.entity.ExchangeRateSource;

public record FetchResult(
    Map<String, BigDecimal> rates, 
    ExchangeRateSource source
) {}