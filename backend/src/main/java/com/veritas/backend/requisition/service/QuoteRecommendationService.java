package com.veritas.backend.requisition.service;

import com.veritas.backend.requisition.dto.QuoteDto;
import com.veritas.backend.requisition.dto.RecommendedQuoteDto;

import java.util.List;

public interface QuoteRecommendationService {

    List<RecommendedQuoteDto> rankQuotes(List<QuoteDto> quotes, Double globalAverageVendorScore);
}
