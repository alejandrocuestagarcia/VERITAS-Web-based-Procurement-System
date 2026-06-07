package com.veritas.backend.integrations.currency.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FrankfurterApiResponse (
    String base,
    String date,
    String quote,
    BigDecimal rate
) {}