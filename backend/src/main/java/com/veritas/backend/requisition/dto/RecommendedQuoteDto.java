package com.veritas.backend.requisition.dto;

public record RecommendedQuoteDto(
    QuoteDto quote,
    double recommendationScore,
    double priceScore,
    double vendorScore,
    double leadTimeScore,
    int rank,
    String recommendationReason
) {
}
