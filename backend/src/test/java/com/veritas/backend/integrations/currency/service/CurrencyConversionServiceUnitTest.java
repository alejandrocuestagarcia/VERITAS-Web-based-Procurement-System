package com.veritas.backend.integrations.currency.service;

import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.integrations.currency.entity.ExchangeRate;
import com.veritas.backend.integrations.currency.repository.ExchangeRateRepository;
import com.veritas.backend.integrations.currency.service.impl.CurrencyConversionServiceImpl;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CurrencyConversionServiceUnitTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private CurrencyConversionServiceImpl service;

    @Test
    void Convert_EUR_ReturnsSameAmount() {
        BigDecimal result = service.convert(BigDecimal.valueOf(100), Currency.EUR);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(100));
        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    void Convert_ValidCurrency_ReturnsConvertedAmount() {
        ExchangeRate rate = new ExchangeRate();
        rate.setRate(BigDecimal.valueOf(2)); // 1 EUR = 2 USD

        when(exchangeRateRepository.findTopByTargetCurrencyOrderByFetchedAtDesc(Currency.USD))
                .thenReturn(Optional.of(rate));

        BigDecimal result = service.convert(BigDecimal.valueOf(100), Currency.USD);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }

    @Test
    void Convert_NullAmount_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> service.convert(null, Currency.USD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be provided and cannot be negative");
    }

    @Test
    void Convert_NegativeAmount_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> service.convert(BigDecimal.valueOf(-1), Currency.USD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be provided and cannot be negative");
    }

    @Test
    void Convert_NullCurrency_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> service.convert(BigDecimal.valueOf(100), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source currency is required");
    }

    @Test
    void Convert_NoExchangeRate_ThrowsEntityNotFound() {
        when(exchangeRateRepository.findTopByTargetCurrencyOrderByFetchedAtDesc(Currency.USD))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.convert(BigDecimal.valueOf(100), Currency.USD))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No exchange rate available");
    }
}
