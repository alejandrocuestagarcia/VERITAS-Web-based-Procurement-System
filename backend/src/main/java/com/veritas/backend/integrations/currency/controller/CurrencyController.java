package com.veritas.backend.integrations.currency.controller;

import com.veritas.backend.config.annotations.IsRequester;
import com.veritas.backend.integrations.currency.dto.CurrencyConversionResponseDto;
import com.veritas.backend.integrations.currency.service.CurrencyConversionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/currency")
@RequiredArgsConstructor
@Tag(name = "Currency Module", description = "Exchange rate retrieval and conversion from persisted data")
public class CurrencyController {

    private final CurrencyConversionService currencyConversionService;

    @Operation(summary = "Convert amount", description = "Converts amount from EUR to target currency using stored rates")
    @IsRequester
    @GetMapping(path = "/convert", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CurrencyConversionResponseDto> convert(@RequestParam BigDecimal amount, @RequestParam String toCurrency) {
        return ResponseEntity.ok(currencyConversionService.convert(amount, toCurrency));
    }
}