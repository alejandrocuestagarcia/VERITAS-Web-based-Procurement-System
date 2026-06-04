package com.veritas.backend.integrations.currency.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRateApiResponse (
    String result,
    @JsonProperty("base_code") String baseCode,
    @JsonProperty("error-type") String errorType,
    @JsonProperty("conversion_rates") Map<String, BigDecimal> conversionRates
) {}