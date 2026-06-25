package com.veritas.backend.requisition.service.impl;

import com.veritas.backend.requisition.dto.QuoteDto;
import com.veritas.backend.requisition.dto.RecommendedQuoteDto;
import com.veritas.backend.requisition.service.QuoteRecommendationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class QuoteRecommendationServiceImpl implements QuoteRecommendationService {

    private static final double WEIGHT_PRICE = 0.55;
    private static final double WEIGHT_VENDOR = 0.30;
    private static final double WEIGHT_LEAD_TIME = 0.15;
    private static final int PRIOR_WEIGHT = 10;
    private static final double MAX_VENDOR_SCORE = 10.0;
    private static final double EXCELLENT_PRICE_THRESHOLD = 0.95;
    private static final double COMPETITIVE_PRICE_THRESHOLD = 0.80;

    private static final double FAST_DELIVERY_THRESHOLD = 0.95;
    private static final double GOOD_DELIVERY_THRESHOLD = 0.80;

    private static final double EXCELLENT_VENDOR_THRESHOLD = 0.85;
    private static final double GOOD_VENDOR_THRESHOLD = 0.70;
    public static final double DEFAULT_GLOBAL_VENDOR_SCORE = 5.0;

    private static final int DIVISION_SCALE = 8;
    private static final double ROUNDING_FACTOR = 10000.0;
    private static final double STRONGEST_FACTOR_THRESHOLD = 0.5;

    @Override
    public List<RecommendedQuoteDto> rankQuotes(List<QuoteDto> quotes, Double globalAverageVendorScore) {
        if (quotes == null || quotes.isEmpty()) {
            return List.of();
        }

        double globalAvg = globalAverageVendorScore != null ? globalAverageVendorScore : DEFAULT_GLOBAL_VENDOR_SCORE;

        BigDecimal minPrice = findMinPrice(quotes);
        int minLeadTime = findMinLeadTime(quotes);

        List<ScoredQuote> scored = new ArrayList<>();
        for (QuoteDto quote : quotes) {
            double priceScore = calculatePriceScore(quote, minPrice);
            double leadTimeScore = calculateLeadTimeScore(quote, minLeadTime);
            double vendorScore = calculateVendorScore(quote, globalAvg);
            double finalScore = WEIGHT_PRICE * priceScore + WEIGHT_VENDOR * vendorScore + WEIGHT_LEAD_TIME * leadTimeScore;

            scored.add(new ScoredQuote(quote, finalScore, priceScore, vendorScore, leadTimeScore));
        }

        scored.sort(Comparator.comparingDouble(ScoredQuote::finalScore).reversed()
                .thenComparing(s -> s.quote().totalAmountEuro() != null ? s.quote().totalAmountEuro() : BigDecimal.ZERO));

        List<RecommendedQuoteDto> result = new ArrayList<>();
        for (int i = 0; i < scored.size(); i++) {
            ScoredQuote s = scored.get(i);
            String reason = generateReason(s, i == 0, quotes.size());
            result.add(new RecommendedQuoteDto(
                    s.quote(),
                    round(s.finalScore()),
                    round(s.priceScore()),
                    round(s.vendorScore()),
                    round(s.leadTimeScore()),
                    i + 1,
                    reason
            ));
        }
        return result;
    }

    private BigDecimal findMinPrice(List<QuoteDto> quotes) {
        BigDecimal min = null;
        for (QuoteDto q : quotes) {
            BigDecimal price = q.totalAmountEuro();
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                if (min == null || price.compareTo(min) < 0) {
                    min = price;
                }
            }
        }
        return min;
    }

    private int findMinLeadTime(List<QuoteDto> quotes) {
        int min = Integer.MAX_VALUE;
        for (QuoteDto q : quotes) {
            Integer time = q.shippingTime();
            if (time != null && time > 0 && time < min) {
                min = time;
            }
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private double calculatePriceScore(QuoteDto quote, BigDecimal minPrice) {
        BigDecimal price = quote.totalAmountEuro();
        if (minPrice == null || price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return minPrice.divide(
                price,
                DIVISION_SCALE,
                RoundingMode.HALF_UP
        ).doubleValue();
    }

    private double calculateLeadTimeScore(QuoteDto quote, int minLeadTime) {
        Integer time = quote.shippingTime();
        if (time == null || time <= 0 || minLeadTime <= 0) {
            return 0.0;
        }
        return (double) minLeadTime / time;
    }

    private double calculateVendorScore(QuoteDto quote, double globalAvg) {
        if (quote.vendor() == null || quote.vendor().overallScore() == null) {
            return globalAvg / MAX_VENDOR_SCORE;
        }

        double rawScore = quote.vendor().overallScore();
        long evalCount = quote.vendor().evaluationCount() != null ? quote.vendor().evaluationCount() : 0L;

        double adjustedScore = (evalCount * rawScore + PRIOR_WEIGHT * globalAvg) / (evalCount + PRIOR_WEIGHT);
        return adjustedScore / MAX_VENDOR_SCORE;
    }

    private String generateReason(ScoredQuote scored, boolean isTopRanked, int totalQuotes) {
        if (totalQuotes == 1) {
            return "Only available quote.";
        }

        if (!isTopRanked) {
            return buildNonTopReason(scored);
        }

        List<String> strengths = new ArrayList<>();

        if (scored.priceScore() >= EXCELLENT_PRICE_THRESHOLD) {
            strengths.add("offers the lowest total price");
        } else if (scored.priceScore() >= COMPETITIVE_PRICE_THRESHOLD) {
            strengths.add("offers a competitive price");
        }

        if (scored.leadTimeScore() >= FAST_DELIVERY_THRESHOLD) {
            strengths.add("provides the fastest delivery");
        } else if (scored.leadTimeScore() >= GOOD_DELIVERY_THRESHOLD) {
            strengths.add("has a short delivery time");
        }

        if (scored.vendorScore() >= EXCELLENT_VENDOR_THRESHOLD) {
            strengths.add("the vendor has an excellent reliability score");
        } else if (scored.vendorScore() >= GOOD_VENDOR_THRESHOLD) {
            strengths.add("the vendor has a strong reliability score");
        }

        if (strengths.isEmpty()) {
            return "Recommended because it provides the best balance between vendor reputation, delivery speed, and overall cost.";
        }

        return "Recommended because it " + String.join(" and ", strengths) + ".";
    }

    private String buildNonTopReason(ScoredQuote scored) {
        boolean hasVendorScore = scored.quote().vendor() != null && scored.quote().vendor().overallScore() != null;

        double maxScore = Math.max(scored.priceScore(), scored.leadTimeScore());
        if (hasVendorScore) {
            maxScore = Math.max(maxScore, scored.vendorScore());
        }

        if (maxScore < STRONGEST_FACTOR_THRESHOLD) {
            return "Less competitive overall.";
        }

        String strongest;
        if (hasVendorScore && scored.vendorScore() >= scored.priceScore() && scored.vendorScore() >= scored.leadTimeScore()) {
            strongest = "vendor reliability";
        } else if (scored.priceScore() >= scored.leadTimeScore()) {
            strongest = "competitive pricing";
        } else {
            strongest = "delivery speed";
        }
        return "Strongest factor: " + strongest + ".";
    }

    private static double round(double value) {
        return Math.round(value * ROUNDING_FACTOR) / ROUNDING_FACTOR;
    }

    private record ScoredQuote(QuoteDto quote, double finalScore, double priceScore, double vendorScore,
                               double leadTimeScore) {
    }
}
