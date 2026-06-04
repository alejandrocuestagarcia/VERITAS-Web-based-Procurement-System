package com.veritas.backend.integrations.currency.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veritas.backend.integrations.currency.entity.ExchangeRate;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findTopByTargetCurrencyOrderByFetchedAtDesc(String targetCurrency);
}