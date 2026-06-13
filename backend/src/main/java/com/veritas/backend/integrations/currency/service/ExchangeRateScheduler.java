package com.veritas.backend.integrations.currency.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that periodically syncs exchange rates via {@link ExchangeRateSyncService}.
 * Runs at a fixed delay controlled by {@code currency.scheduler-interval-ms} (default: 1 hour),
 * with an initial delay of 10 seconds on startup.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    private final ExchangeRateSyncService exchangeRateSyncService;

    @Scheduled(fixedDelayString = "${currency.scheduler-interval-ms:3600000}", initialDelay = 10000)
    public void runScheduledSync() {
        log.info("Running scheduled exchange rate sync");
        executeExchangeRateSync();
    }

    private void executeExchangeRateSync() {
        try {
            exchangeRateSyncService.fetchAndStoreLatestExchangeRates();
        } catch (RuntimeException exception) {
            log.error("Scheduled exchange rate sync failed: {}", exception.getMessage(), exception);
        }
    }
}