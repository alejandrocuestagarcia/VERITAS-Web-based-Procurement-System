package com.veritas.backend.requisition.service;

import com.veritas.backend.requisition.dto.QuoteDto;
import com.veritas.backend.requisition.dto.RecommendedQuoteDto;

import java.util.List;

public interface QuoteRecommendationService {

    /**
     * Ranks vendor quotes based on a weighted recommendation score.
     * The score is calculated using price score (55%), vendor reliability score (30%),
     * and delivery lead time score (15%).
     *
     * @param quotes                     the list of quote DTOs to rank
     * @param globalAverageVendorScore   the global average vendor score used for Bayesian prior adjustment,
     *                                   or null if no average score is available
     * @return a sorted list of recommended quote DTOs, ordered by their recommendation score descending
     */
    List<RecommendedQuoteDto> rankQuotes(List<QuoteDto> quotes, Double globalAverageVendorScore);
}
