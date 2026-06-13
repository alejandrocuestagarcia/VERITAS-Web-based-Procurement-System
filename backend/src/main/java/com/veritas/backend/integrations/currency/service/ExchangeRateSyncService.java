package com.veritas.backend.integrations.currency.service;

public interface ExchangeRateSyncService {

    /**
     * Fetches the latest exchange rates from an external provider and persists them to the database.
     * Tries the primary provider (Frankfurter) first, falling back to the secondary (ExchangeRateAPI) on failure.
     * If both providers fail, the existing data is left untouched.
     * Target currencies are controlled by the {@code currency.target-currencies} configuration property.
     */
    void fetchAndStoreLatestExchangeRates();
}