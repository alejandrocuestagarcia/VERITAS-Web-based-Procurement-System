package com.veritas.backend.integrations.currency.service;

public interface ExchangeRateSyncService {
    void fetchAndStoreLatestExchangeRates();
}