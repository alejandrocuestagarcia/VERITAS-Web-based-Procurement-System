package com.veritas.backend.integrations.currency.service.impl;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.util.UriComponentsBuilder;

import com.veritas.backend.integrations.currency.dto.ExchangeRateApiResponse;
import com.veritas.backend.integrations.currency.dto.FetchResult;
import com.veritas.backend.integrations.currency.dto.FrankfurterApiResponse;
import com.veritas.backend.integrations.currency.entity.ExchangeRate;
import com.veritas.backend.integrations.currency.entity.ExchangeRateSource;
import com.veritas.backend.integrations.currency.repository.ExchangeRateRepository;
import com.veritas.backend.integrations.currency.service.ExchangeRateSyncService;

@Service
@Slf4j
public class ExchangeRateSyncServiceImpl implements ExchangeRateSyncService {

    private static final String BASE_CURRENCY = "EUR";

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    private RestTemplate restTemplate;

    @Value("${currency.primary-url}")
    private String primaryApiUrl;

    @Value("${currency.secondary-url}")
    private String secondaryApiUrl;

    @Value("${currency.secondary-api-key:}")
    private String secondaryApiKey;

    @Value("${currency.target-currencies:USD,GBP,CHF,JPY}")
    private String targetCurrenciesRaw;

    @PostConstruct
    private void initRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @Override
    @Transactional
    public void fetchAndStoreLatestExchangeRates() {
        List<String> targetCurrencies = normalizedTargetCurrencies();

        if (targetCurrencies.isEmpty()) {
            log.warn("No target currencies configured. Skipping exchange rate sync");
            return;
        }

        Optional<FetchResult> primaryResult = fetchFromFrankfurterApi(targetCurrencies);
        Optional<FetchResult> successfulResult = primaryResult.isPresent() ? primaryResult : fetchFromExchangeRateApi(targetCurrencies);

        if (successfulResult.isEmpty()) {
            log.error("Could not fetch exchange rates from either provider. Keeping existing DB data");
            return;
        }

        persistExchangeRates(targetCurrencies, successfulResult.get());
    }

    private void persistExchangeRates(List<String> targetCurrencies, FetchResult result) {
        LocalDateTime fetchedAt = LocalDateTime.now();
        List<ExchangeRate> entities = new ArrayList<>();

        for (String targetCurrency : targetCurrencies) {
            BigDecimal rate = result.rates().get(targetCurrency);
            if (rate == null) {
                log.warn("Rate missing for target currency {} from source {}.", targetCurrency, result.source());
                continue;
            }

            entities.add(ExchangeRate.builder()
                .targetCurrency(targetCurrency)
                .rate(rate)
                .fetchedAt(fetchedAt)
                .source(result.source())
                .build());
        }

        if (entities.isEmpty()) {
            log.error("No rates were persisted because provider payload had no usable target currencies.");
            return;
        }

        exchangeRateRepository.saveAll(entities);
        log.info("Saved {} exchange rates using source {} at {}.", entities.size(), result.source(), fetchedAt);
    }

    private Optional<FetchResult> fetchFromFrankfurterApi(List<String> targetCurrencies) {
        String url = UriComponentsBuilder
            .fromHttpUrl(primaryApiUrl)
            .queryParam("base", BASE_CURRENCY)
            .queryParam("quotes", String.join(",", targetCurrencies))
            .toUriString();

        try {
            ResponseEntity<FrankfurterApiResponse[]> response = restTemplate.getForEntity(url, FrankfurterApiResponse[].class);

            FrankfurterApiResponse[] body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null || body.length == 0) {
                throw new IllegalStateException("Frankfurter returned an invalid response payload");
            }

            Map<String, BigDecimal> rawExchangeRates = new LinkedHashMap<>();
            for (FrankfurterApiResponse entry : body) {
                if (entry == null) {
                    continue;
                }

                if (!BASE_CURRENCY.equalsIgnoreCase(entry.base())) {
                    throw new IllegalStateException("Frankfurter returned unexpected base currency: " + entry.base());
                }

                if (!StringUtils.hasText(entry.quote()) || entry.rate() == null) {
                    continue;
                }

                rawExchangeRates.put(entry.quote(), entry.rate());
            }

            Map<String, BigDecimal> normalizedRates = normalizeExchangeRates(rawExchangeRates);
            if (normalizedRates.isEmpty()) {
                throw new IllegalStateException("Frankfurter returned no usable exchange rates");
            }

            return Optional.of(new FetchResult(normalizedRates, ExchangeRateSource.FRANKFURTER));
        } catch (Exception exception) {
            log.warn("Primary exchange currency provider (Frankfurter) failed: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<FetchResult> fetchFromExchangeRateApi(List<String> targetCurrencies) {
        if (!StringUtils.hasText(secondaryApiKey)) {
            log.warn("Secondary exchange currency provider is not configured with a valid API key");
            return Optional.empty();
        }

        String url = String.format("%s/%s/latest/%s", secondaryApiUrl, secondaryApiKey, BASE_CURRENCY);

        try {
            ResponseEntity<ExchangeRateApiResponse> response = restTemplate.getForEntity(url, ExchangeRateApiResponse.class);

            ExchangeRateApiResponse body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                throw new IllegalStateException("ExchangeRateAPI returned an invalid response payload");
            }

            if (!"success".equalsIgnoreCase(body.result())) {
                log.warn("ExchangeRateAPI failed with error type: {}", body.errorType());
                return Optional.empty();
            }

            if (!BASE_CURRENCY.equalsIgnoreCase(body.baseCode())) {
                throw new IllegalStateException("ExchangeRateAPI returned unexpected base currency: " + body.baseCode());
            }

            Map<String, BigDecimal> normalizedRates = normalizeExchangeRates(body.conversionRates());
            if (normalizedRates.isEmpty()) {
                throw new IllegalStateException("ExchangeRateAPI returned no usable exchange rates");
            }

            return Optional.of(new FetchResult(filterToTargets(normalizedRates, targetCurrencies), ExchangeRateSource.EXCHANGERATE_API));
        } catch (Exception exception) {
            log.warn("Secondary exchange currency provider (ExchangeRateAPI) failed: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, BigDecimal> normalizeExchangeRates(Map<String, BigDecimal> sourceRates) {
        Map<String, BigDecimal> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : sourceRates.entrySet()) {
            if (!StringUtils.hasText(entry.getKey()) || entry.getValue() == null) {
                continue;
            }

            BigDecimal value = entry.getValue();
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            normalized.put(entry.getKey().trim().toUpperCase(), value);
        }

        return normalized;
    }

    private Map<String, BigDecimal> filterToTargets(Map<String, BigDecimal> allRates, List<String> targetCurrencies) {
        Map<String, BigDecimal> filtered = new LinkedHashMap<>();
        for (String targetCurrency : targetCurrencies) {
            BigDecimal value = allRates.get(targetCurrency);

            if (value != null) {
                filtered.put(targetCurrency, value);
            }
        }

        return filtered;
    }

    private List<String> normalizedTargetCurrencies() {
        return Arrays.stream(targetCurrenciesRaw.split(","))
            .filter(StringUtils::hasText)
            .map(currency -> currency.trim().toUpperCase())
            .distinct()
            .toList();
    }

}