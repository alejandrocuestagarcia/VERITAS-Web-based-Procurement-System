package com.veritas.backend.integrations.currency.service;

import java.util.Map;

import com.veritas.backend.integrations.currency.dto.ExchangeRateApiResponse;
import com.veritas.backend.integrations.currency.dto.FrankfurterApiResponse;
import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.integrations.currency.entity.ExchangeRate;
import com.veritas.backend.integrations.currency.entity.ExchangeRateSource;
import com.veritas.backend.integrations.currency.repository.ExchangeRateRepository;
import com.veritas.backend.integrations.currency.service.impl.ExchangeRateSyncServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateSyncServiceUnitTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExchangeRateSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);

        ReflectionTestUtils.setField(service, "primaryApiUrl", "http://primary/api");
        ReflectionTestUtils.setField(service, "secondaryApiUrl", "http://secondary/api");
        ReflectionTestUtils.setField(service, "secondaryApiKey", "secret-key");

        ReflectionTestUtils.setField(service, "targetCurrenciesRaw", "USD,GBP,CHF,JPY");
    }

    @Test
    void FetchAndStore_PrimaryProviderSuccess_PersistsRates() {
        FrankfurterApiResponse[] response = new FrankfurterApiResponse[] {
                new FrankfurterApiResponse("EUR", "2025-01-01", "USD", BigDecimal.valueOf(1.1)),
                new FrankfurterApiResponse("EUR", "2025-01-01", "GBP", BigDecimal.valueOf(0.9)),
                new FrankfurterApiResponse("EUR", "2025-01-01", "CHF", BigDecimal.valueOf(0.95)),
                new FrankfurterApiResponse("EUR", "2025-01-01", "JPY", BigDecimal.valueOf(130.0)),
        };

        when(restTemplate.getForEntity(anyString(), eq(FrankfurterApiResponse[].class))).thenReturn(ResponseEntity.ok(response));

        service.fetchAndStoreLatestExchangeRates();

        ArgumentCaptor<List<ExchangeRate>> captor = ArgumentCaptor.forClass(List.class);
        verify(exchangeRateRepository, times(1)).saveAll(captor.capture());

        List<ExchangeRate> saved = captor.getValue();
        assertEquals(4, saved.size());

        List<Currency> currencies = saved.stream().map(ExchangeRate::getTargetCurrency).toList();
        assertAll(
            () -> assertTrue(currencies.contains(Currency.USD)),
            () -> assertTrue(currencies.contains(Currency.GBP)),
            () -> assertTrue(currencies.contains(Currency.CHF)),
            () -> assertTrue(currencies.contains(Currency.JPY)),
            () -> assertTrue(saved.stream().allMatch(e -> e.getRate().compareTo(BigDecimal.ZERO) > 0)),
            () -> assertTrue(saved.stream().allMatch(e -> e.getSource() == ExchangeRateSource.FRANKFURTER))
        );
    }

    @Test
    void FetchAndStore_PrimaryFailsAndSecondarySuccess_PersistsRates() {
        when(restTemplate.getForEntity(anyString(), eq(FrankfurterApiResponse[].class))).thenThrow(new RuntimeException("Primary API is down"));

        ExchangeRateApiResponse secondary = new ExchangeRateApiResponse(
                "success",
                "EUR",
                null,
                Map.of(
                        "USD", BigDecimal.valueOf(1.2),
                        "GBP", BigDecimal.valueOf(0.8),
                        "CHF", BigDecimal.valueOf(0.9),
                        "JPY", BigDecimal.valueOf(125.0)
                )
        );

        when(restTemplate.getForEntity(anyString(), eq(ExchangeRateApiResponse.class))).thenReturn(ResponseEntity.ok(secondary));

        service.fetchAndStoreLatestExchangeRates();

        ArgumentCaptor<List<ExchangeRate>> captor = ArgumentCaptor.forClass(List.class);
        verify(exchangeRateRepository, times(1)).saveAll(captor.capture());

        List<ExchangeRate> saved = captor.getValue();
        assertEquals(4, saved.size());

        List<Currency> currencies = saved.stream().map(ExchangeRate::getTargetCurrency).toList();
        assertAll(
            () -> assertTrue(currencies.contains(Currency.USD)),
            () -> assertTrue(currencies.contains(Currency.GBP)),
            () -> assertTrue(currencies.contains(Currency.CHF)),
            () -> assertTrue(currencies.contains(Currency.JPY)),
            () -> assertTrue(saved.stream().allMatch(e -> e.getRate().compareTo(BigDecimal.ZERO) > 0)),
            () -> assertTrue(saved.stream().allMatch(e -> e.getSource() == ExchangeRateSource.EXCHANGERATE_API))
        );
    }

    @Test
    void FetchAndStore_BothProvidersFail_NoPersistence() {
        when(restTemplate.getForEntity(anyString(), any())).thenThrow(new RuntimeException("API down"));

        service.fetchAndStoreLatestExchangeRates();

        verify(exchangeRateRepository, never()).saveAll(any());
    }

    @Test
    void FetchAndStore_PrimaryInvalidBase_FallsBackToSecondary() {
        FrankfurterApiResponse[] invalid = new FrankfurterApiResponse[] {
                new FrankfurterApiResponse("WRONG_BASE", "2025-01-01", "USD", BigDecimal.valueOf(1.1))
        };

        when(restTemplate.getForEntity(anyString(), eq(FrankfurterApiResponse[].class))).thenReturn(ResponseEntity.ok(invalid));

        ExchangeRateApiResponse secondary = new ExchangeRateApiResponse(
                "success",
                "EUR",
                null,
                Map.of("USD", BigDecimal.valueOf(1.2))
        );

        when(restTemplate.getForEntity(anyString(), eq(ExchangeRateApiResponse.class))).thenReturn(ResponseEntity.ok(secondary));

        service.fetchAndStoreLatestExchangeRates();

        ArgumentCaptor<List<ExchangeRate>> captor = ArgumentCaptor.forClass(List.class);
        verify(exchangeRateRepository, times(1)).saveAll(captor.capture());

        List<ExchangeRate> saved = captor.getValue();
        assertEquals(1, saved.size());

        List<Currency> currencies = saved.stream().map(ExchangeRate::getTargetCurrency).toList();
        assertAll(
            () -> assertTrue(currencies.contains(Currency.USD)),
            () -> assertTrue(saved.stream().allMatch(e -> e.getRate().compareTo(BigDecimal.ZERO) > 0)),
            () -> assertTrue(saved.stream().allMatch(e -> e.getSource() == ExchangeRateSource.EXCHANGERATE_API))
        );
    }

    @Test
    void FetchAndStore_UnsupportedCurrency_IsIgnored() {
        FrankfurterApiResponse[] response = new FrankfurterApiResponse[] {
                new FrankfurterApiResponse("EUR", "2025-01-01", "USD", BigDecimal.valueOf(1.1)),
                new FrankfurterApiResponse("EUR", "2025-01-01", "XYZ", BigDecimal.valueOf(2.0))
        };

        when(restTemplate.getForEntity(anyString(), eq(FrankfurterApiResponse[].class))).thenReturn(ResponseEntity.ok(response));

        service.fetchAndStoreLatestExchangeRates();

        ArgumentCaptor<List<ExchangeRate>> captor = ArgumentCaptor.forClass(List.class);
        verify(exchangeRateRepository).saveAll(captor.capture());

        List<ExchangeRate> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertEquals(Currency.USD, saved.get(0).getTargetCurrency());
    }

    @Test
    void FetchAndStore_NoTargetCurrencies_NoExecution() {
        ReflectionTestUtils.setField(service, "targetCurrenciesRaw", "");

        service.fetchAndStoreLatestExchangeRates();

        verify(exchangeRateRepository, never()).saveAll(any());
    }

    @Test
    void FetchAndStore_PrimaryResponseNot2xx_FallsBackToSecondary() {
        when(restTemplate.getForEntity(anyString(), eq(FrankfurterApiResponse[].class)))
                .thenReturn(ResponseEntity.status(500).build());

        ExchangeRateApiResponse secondary = new ExchangeRateApiResponse(
                "success", "EUR", null, Map.of("USD", BigDecimal.valueOf(1.2))
        );
        when(restTemplate.getForEntity(anyString(), eq(ExchangeRateApiResponse.class)))
                .thenReturn(ResponseEntity.ok(secondary));

        service.fetchAndStoreLatestExchangeRates();

        verify(exchangeRateRepository, times(1)).saveAll(anyList());
    }

    @Test
    void FetchAndStore_PrimaryBodyNullOrEmpty_FallsBackToSecondary() {
        when(restTemplate.getForEntity(anyString(), eq(FrankfurterApiResponse[].class)))
                .thenReturn(ResponseEntity.ok((FrankfurterApiResponse[]) null));

        ExchangeRateApiResponse secondary = new ExchangeRateApiResponse(
                "success", "EUR", null, Map.of("USD", BigDecimal.valueOf(1.2))
        );
        when(restTemplate.getForEntity(anyString(), eq(ExchangeRateApiResponse.class)))
                .thenReturn(ResponseEntity.ok(secondary));

        service.fetchAndStoreLatestExchangeRates();

        verify(exchangeRateRepository, times(1)).saveAll(anyList());
    }

    @Test
    void FetchAndStore_PrimaryEntryNullOrQuoteBlankOrRateNull_FiltersThemOut() {
        FrankfurterApiResponse[] response = new FrankfurterApiResponse[] {
                null,
                new FrankfurterApiResponse("EUR", "2025-01-01", "", BigDecimal.valueOf(1.1)),
                new FrankfurterApiResponse("EUR", "2025-01-01", "USD", null),
                new FrankfurterApiResponse("EUR", "2025-01-01", "GBP", BigDecimal.valueOf(0.9)),
        };

        when(restTemplate.getForEntity(anyString(), eq(FrankfurterApiResponse[].class)))
                .thenReturn(ResponseEntity.ok(response));

        service.fetchAndStoreLatestExchangeRates();

        ArgumentCaptor<List<ExchangeRate>> captor = ArgumentCaptor.forClass(List.class);
        verify(exchangeRateRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(Currency.GBP, captor.getValue().get(0).getTargetCurrency());
    }

    @Test
    void FetchAndStore_PrimaryNoUsableRates_FallsBackToSecondary() {
        FrankfurterApiResponse[] response = new FrankfurterApiResponse[] {
                new FrankfurterApiResponse("EUR", "2025-01-01", "USD", BigDecimal.valueOf(-1.1))
        };

        when(restTemplate.getForEntity(anyString(), eq(FrankfurterApiResponse[].class)))
                .thenReturn(ResponseEntity.ok(response));

        ExchangeRateApiResponse secondary = new ExchangeRateApiResponse(
                "success", "EUR", null, Map.of("GBP", BigDecimal.valueOf(0.9))
        );
        when(restTemplate.getForEntity(anyString(), eq(ExchangeRateApiResponse.class)))
                .thenReturn(ResponseEntity.ok(secondary));

        service.fetchAndStoreLatestExchangeRates();

        ArgumentCaptor<List<ExchangeRate>> captor = ArgumentCaptor.forClass(List.class);
        verify(exchangeRateRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(Currency.GBP, captor.getValue().get(0).getTargetCurrency());
    }

    @Test
    void FetchAndStore_SecondaryApiKeyMissing_SkipsSecondary() {
        when(restTemplate.getForEntity(anyString(), eq(FrankfurterApiResponse[].class)))
                .thenThrow(new RuntimeException("Primary fails"));
        ReflectionTestUtils.setField(service, "secondaryApiKey", "");

        service.fetchAndStoreLatestExchangeRates();

        verify(exchangeRateRepository, never()).saveAll(any());
    }

    @Test
    void FetchAndStore_SecondaryResponseNot2xx_NoPersistence() {
        when(restTemplate.getForEntity(anyString(), eq(FrankfurterApiResponse[].class)))
                .thenThrow(new RuntimeException("Primary fails"));
        when(restTemplate.getForEntity(anyString(), eq(ExchangeRateApiResponse.class)))
                .thenReturn(ResponseEntity.status(500).build());

        service.fetchAndStoreLatestExchangeRates();

        verify(exchangeRateRepository, never()).saveAll(any());
    }

    @Test
    void FetchAndStore_SecondaryResultNotSuccess_NoPersistence() {
        when(restTemplate.getForEntity(anyString(), eq(FrankfurterApiResponse[].class)))
                .thenThrow(new RuntimeException("Primary fails"));

        ExchangeRateApiResponse secondary = new ExchangeRateApiResponse(
                "error", "EUR", "invalid-key", Map.of("USD", BigDecimal.valueOf(1.2))
        );
        when(restTemplate.getForEntity(anyString(), eq(ExchangeRateApiResponse.class)))
                .thenReturn(ResponseEntity.ok(secondary));

        service.fetchAndStoreLatestExchangeRates();

        verify(exchangeRateRepository, never()).saveAll(any());
    }

    @Test
    void FetchAndStore_SecondaryInvalidBase_NoPersistence() {
        when(restTemplate.getForEntity(anyString(), eq(FrankfurterApiResponse[].class)))
                .thenThrow(new RuntimeException("Primary fails"));

        ExchangeRateApiResponse secondary = new ExchangeRateApiResponse(
                "success", "USD", null, Map.of("EUR", BigDecimal.valueOf(1.2))
        );
        when(restTemplate.getForEntity(anyString(), eq(ExchangeRateApiResponse.class)))
                .thenReturn(ResponseEntity.ok(secondary));

        service.fetchAndStoreLatestExchangeRates();

        verify(exchangeRateRepository, never()).saveAll(any());
    }

    @Test
    void FetchAndStore_SecondaryNoUsableRates_NoPersistence() {
        when(restTemplate.getForEntity(anyString(), eq(FrankfurterApiResponse[].class)))
                .thenThrow(new RuntimeException("Primary fails"));

        ExchangeRateApiResponse secondary = new ExchangeRateApiResponse(
                "success", "EUR", null, Map.of("USD", BigDecimal.valueOf(-1.2))
        );
        when(restTemplate.getForEntity(anyString(), eq(ExchangeRateApiResponse.class)))
                .thenReturn(ResponseEntity.ok(secondary));

        service.fetchAndStoreLatestExchangeRates();

        verify(exchangeRateRepository, never()).saveAll(any());
    }

    @Test
    void FetchAndStore_ZeroOrNegativeRates_AreIgnored() {
        FrankfurterApiResponse[] response = new FrankfurterApiResponse[] {
                new FrankfurterApiResponse("EUR", "2025-01-01", "USD", BigDecimal.ZERO),
                new FrankfurterApiResponse("EUR", "2025-01-01", "GBP", BigDecimal.valueOf(-0.9)),
                new FrankfurterApiResponse("EUR", "2025-01-01", "CHF", BigDecimal.valueOf(0.95))
        };

        when(restTemplate.getForEntity(anyString(), eq(FrankfurterApiResponse[].class)))
                .thenReturn(ResponseEntity.ok(response));

        service.fetchAndStoreLatestExchangeRates();

        ArgumentCaptor<List<ExchangeRate>> captor = ArgumentCaptor.forClass(List.class);
        verify(exchangeRateRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(Currency.CHF, captor.getValue().get(0).getTargetCurrency());
    }
}
