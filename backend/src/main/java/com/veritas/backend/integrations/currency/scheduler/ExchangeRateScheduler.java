package com.veritas.backend.integrations.currency.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.veritas.backend.integrations.currency.service.ExchangeRateSyncService;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    private final ExchangeRateSyncService exchangeRateSyncService;

    @EventListener(ApplicationReadyEvent.class)
    public void runInitialSync() {
        log.info("Running initial exchange-rate sync on application startup");
        executeExchangeRateSync();
    }

    @Scheduled(fixedDelayString = "${currency.scheduler-interval-ms:3600000}", initialDelay = 10000)
    public void runScheduledSync() {
        executeExchangeRateSync();
    }

    private void executeExchangeRateSync() {
        try {
            exchangeRateSyncService.fetchAndStoreLatestExchangeRates();
        } catch (RuntimeException exception) {
            log.error("Scheduled exchange-rate sync failed: {}", exception.getMessage(), exception);
        }
    }
}