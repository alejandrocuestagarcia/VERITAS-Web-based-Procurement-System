package com.veritas.backend.requisition.service;

import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.requisition.dto.QuoteDto;
import com.veritas.backend.requisition.dto.RecommendedQuoteDto;
import com.veritas.backend.requisition.service.impl.QuoteRecommendationServiceImpl;
import com.veritas.backend.vendor.dto.VendorDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//AI-GENERATED
class QuoteRecommendationServiceUnitTest {

    private QuoteRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new QuoteRecommendationServiceImpl();
    }

    // --- Helper builders ---

    private VendorDto vendor(double overallScore, long evaluationCount) {
        return new VendorDto(1L, "Vendor", "TAX123", null, null, null, null, overallScore, evaluationCount,
                "Description", null, null, null, null, null);
    }

    private VendorDto vendorNoScore() {
        return new VendorDto(1L, "Vendor", "TAX123", null, null, null, null, null, null,
                "Description", null, null, null, null, null);
    }

    private QuoteDto quote(BigDecimal totalEuro, int shippingTime, VendorDto vendor) {
        return QuoteDto.builder()
                .quoteId(1L)
                .vendorId(vendor.id())
                .vendor(vendor)
                .currency(Currency.EUR)
                .baseAmount(totalEuro)
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(totalEuro)
                .totalAmountEuro(totalEuro)
                .shippingTime(shippingTime)
                .isSelected(false)
                .items(List.of())
                .build();
    }

    // --- Tests ---

    @Test
    void rankQuotes_emptyList_returnsEmpty() {
        List<RecommendedQuoteDto> result = service.rankQuotes(List.of(), 7.5);
        assertTrue(result.isEmpty());
    }

    @Test
    void rankQuotes_nullList_returnsEmpty() {
        List<RecommendedQuoteDto> result = service.rankQuotes(null, 7.5);
        assertTrue(result.isEmpty());
    }

    @Test
    void rankQuotes_singleQuote_returnsRank1WithOnlyAvailableReason() {
        VendorDto v = vendor(8.0, 50);
        QuoteDto q = quote(BigDecimal.valueOf(1000), 5, v);

        List<RecommendedQuoteDto> result = service.rankQuotes(List.of(q), 7.5);

        assertEquals(1, result.size());
        RecommendedQuoteDto rec = result.getFirst();
        assertEquals(1, rec.rank());
        assertEquals(1.0, rec.priceScore());
        assertEquals(1.0, rec.leadTimeScore());
        assertEquals("Only available quote.", rec.recommendationReason());
    }

    @Test
    void rankQuotes_cheapestQuoteGetsPriceScore1() {
        VendorDto v1 = vendor(7.0, 50);
        VendorDto v2 = vendor(7.0, 50);
        QuoteDto cheap = quote(BigDecimal.valueOf(500), 10, v1);
        QuoteDto expensive = quote(BigDecimal.valueOf(1000), 10, v2);

        List<RecommendedQuoteDto> result = service.rankQuotes(List.of(expensive, cheap), 7.0);

        // Find the cheap quote
        RecommendedQuoteDto cheapRec = result.stream()
                .filter(r -> r.quote().totalAmountEuro().compareTo(BigDecimal.valueOf(500)) == 0)
                .findFirst().orElseThrow();
        RecommendedQuoteDto expensiveRec = result.stream()
                .filter(r -> r.quote().totalAmountEuro().compareTo(BigDecimal.valueOf(1000)) == 0)
                .findFirst().orElseThrow();

        assertEquals(1.0, cheapRec.priceScore());
        assertEquals(0.5, expensiveRec.priceScore());
    }

    @Test
    void rankQuotes_fastestQuoteGetsLeadTimeScore1() {
        VendorDto v1 = vendor(7.0, 50);
        VendorDto v2 = vendor(7.0, 50);
        QuoteDto fast = quote(BigDecimal.valueOf(1000), 3, v1);
        QuoteDto slow = quote(BigDecimal.valueOf(1000), 9, v2);

        List<RecommendedQuoteDto> result = service.rankQuotes(List.of(slow, fast), 7.0);

        RecommendedQuoteDto fastRec = result.stream()
                .filter(r -> r.quote().shippingTime() == 3)
                .findFirst().orElseThrow();
        RecommendedQuoteDto slowRec = result.stream()
                .filter(r -> r.quote().shippingTime() == 9)
                .findFirst().orElseThrow();

        assertEquals(1.0, fastRec.leadTimeScore());
        double expectedSlowScore = 3.0 / 9.0;
        assertEquals(expectedSlowScore, slowRec.leadTimeScore(), 0.001);
    }

    @Test
    void rankQuotes_bayesianAdjustmentReducesLowEvalCountVendor() {
        // Vendor A: overallScore=10.0, evaluations=1 -> Bayesian pulls toward global avg
        // Vendor B: overallScore=9.0, evaluations=150 -> Bayesian barely changes score
        VendorDto vendorA = vendor(10.0, 1);
        VendorDto vendorB = vendor(9.0, 150);
        QuoteDto qA = quote(BigDecimal.valueOf(1000), 5, vendorA);
        QuoteDto qB = quote(BigDecimal.valueOf(1000), 5, vendorB);

        List<RecommendedQuoteDto> result = service.rankQuotes(List.of(qA, qB), 7.5);

        // With same price and lead time, vendor score determines ranking
        // Vendor A Bayesian: (1*10 + 10*7.5) / 11 = 85/11 ≈ 7.727 -> vendorScore = 0.7727
        // Vendor B Bayesian: (150*9 + 10*7.5) / 160 = 1425/160 ≈ 8.906 -> vendorScore = 0.8906
        // Vendor B should rank higher
        RecommendedQuoteDto recA = result.stream()
                .filter(r -> r.quote().vendor().evaluationCount() == 1)
                .findFirst().orElseThrow();
        RecommendedQuoteDto recB = result.stream()
                .filter(r -> r.quote().vendor().evaluationCount() == 150)
                .findFirst().orElseThrow();

        assertTrue(recB.vendorScore() > recA.vendorScore(),
                "Vendor B (many evals) should have higher vendor score than Vendor A (1 eval)");
        assertEquals(1, recB.rank(), "Vendor B should be ranked first");
        assertEquals(2, recA.rank(), "Vendor A should be ranked second");
    }

    @Test
    void rankQuotes_zeroShippingTime_getsLeadTimeScore0() {
        VendorDto v = vendor(7.0, 50);
        QuoteDto q = quote(BigDecimal.valueOf(1000), 0, v);

        List<RecommendedQuoteDto> result = service.rankQuotes(List.of(q), 7.0);

        assertEquals(0.0, result.getFirst().leadTimeScore());
    }

    @Test
    void rankQuotes_nullVendorScore_usesGlobalAverage() {
        VendorDto v = vendorNoScore();
        QuoteDto q = quote(BigDecimal.valueOf(1000), 5, v);

        List<RecommendedQuoteDto> result = service.rankQuotes(List.of(q), 7.5);

        // With null vendor score, vendorScore should be globalAvg / 10.0 = 0.75
        assertEquals(0.75, result.getFirst().vendorScore());
    }

    @Test
    void rankQuotes_deterministicRanking() {
        VendorDto v1 = vendor(8.0, 100);
        VendorDto v2 = vendor(6.0, 80);
        VendorDto v3 = vendor(9.0, 30);
        QuoteDto q1 = quote(BigDecimal.valueOf(800), 7, v1);
        QuoteDto q2 = quote(BigDecimal.valueOf(600), 14, v2);
        QuoteDto q3 = quote(BigDecimal.valueOf(1200), 3, v3);

        // Run multiple times and ensure same order
        List<RecommendedQuoteDto> result1 = service.rankQuotes(List.of(q1, q2, q3), 7.0);
        List<RecommendedQuoteDto> result2 = service.rankQuotes(List.of(q3, q1, q2), 7.0);

        for (int i = 0; i < 3; i++) {
            assertEquals(result1.get(i).quote().quoteId(), result2.get(i).quote().quoteId(),
                    "Ranking must be deterministic regardless of input order");
            assertEquals(result1.get(i).rank(), result2.get(i).rank());
            assertEquals(result1.get(i).recommendationScore(), result2.get(i).recommendationScore());
        }
    }

    @Test
    void rankQuotes_ranksAreConsecutive() {
        VendorDto v1 = vendor(7.0, 50);
        VendorDto v2 = vendor(8.0, 60);
        VendorDto v3 = vendor(6.0, 40);
        QuoteDto q1 = quote(BigDecimal.valueOf(1000), 5, v1);
        QuoteDto q2 = quote(BigDecimal.valueOf(800), 7, v2);
        QuoteDto q3 = quote(BigDecimal.valueOf(1200), 3, v3);

        List<RecommendedQuoteDto> result = service.rankQuotes(List.of(q1, q2, q3), 7.0);

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).rank());
        assertEquals(2, result.get(1).rank());
        assertEquals(3, result.get(2).rank());
    }

    @Test
    void rankQuotes_scoresAreBetween0And1() {
        VendorDto v1 = vendor(9.5, 200);
        VendorDto v2 = vendor(3.0, 10);
        QuoteDto q1 = quote(BigDecimal.valueOf(500), 2, v1);
        QuoteDto q2 = quote(BigDecimal.valueOf(5000), 30, v2);

        List<RecommendedQuoteDto> result = service.rankQuotes(List.of(q1, q2), 6.0);

        for (RecommendedQuoteDto rec : result) {
            assertTrue(rec.priceScore() >= 0.0 && rec.priceScore() <= 1.0,
                    "priceScore must be between 0 and 1");
            assertTrue(rec.vendorScore() >= 0.0 && rec.vendorScore() <= 1.0,
                    "vendorScore must be between 0 and 1");
            assertTrue(rec.leadTimeScore() >= 0.0 && rec.leadTimeScore() <= 1.0,
                    "leadTimeScore must be between 0 and 1");
            assertTrue(rec.recommendationScore() >= 0.0 && rec.recommendationScore() <= 1.0,
                    "recommendationScore must be between 0 and 1");
        }
    }

    @Test
    void rankQuotes_topRecommendedQuoteHasExplanation() {
        VendorDto v1 = vendor(9.0, 100);
        VendorDto v2 = vendor(5.0, 20);
        QuoteDto q1 = quote(BigDecimal.valueOf(500), 3, v1);
        QuoteDto q2 = quote(BigDecimal.valueOf(1500), 10, v2);

        List<RecommendedQuoteDto> result = service.rankQuotes(List.of(q1, q2), 7.0);

        RecommendedQuoteDto top = result.getFirst();
        assertNotNull(top.recommendationReason());
        assertTrue(top.recommendationReason().startsWith("Recommended"),
                "Top-ranked quote should start with 'Recommended'");
    }

    @Test
    void rankQuotes_nonTopQuoteHasStrongestFactorExplanation() {
        VendorDto v1 = vendor(9.0, 100);
        VendorDto v2 = vendor(5.0, 20);
        QuoteDto q1 = quote(BigDecimal.valueOf(500), 3, v1);
        QuoteDto q2 = quote(BigDecimal.valueOf(1500), 10, v2);

        List<RecommendedQuoteDto> result = service.rankQuotes(List.of(q1, q2), 7.0);

        RecommendedQuoteDto nonTop = result.get(1);
        assertNotNull(nonTop.recommendationReason());
        assertTrue(nonTop.recommendationReason().startsWith("Strongest factor"),
                "Non-top quote should explain its strongest factor");
    }

    @Test
    void rankQuotes_nullGlobalAverage_usesDefault() {
        VendorDto v = vendor(8.0, 50);
        QuoteDto q = quote(BigDecimal.valueOf(1000), 5, v);

        List<RecommendedQuoteDto> result = service.rankQuotes(List.of(q), null);

        assertFalse(result.isEmpty());
        assertTrue(result.getFirst().recommendationScore() > 0);
    }

    @Test
    void rankQuotes_correctWeights() {
        // Single quote with known values to verify weight formula
        VendorDto v = vendor(10.0, 1000); // effectively converges to 10.0 with Bayesian
        QuoteDto q = quote(BigDecimal.valueOf(1000), 5, v);

        List<RecommendedQuoteDto> result = service.rankQuotes(List.of(q), 10.0);

        RecommendedQuoteDto rec = result.getFirst();
        // priceScore = 1.0 (only quote), leadTimeScore = 1.0 (only quote), vendorScore ≈ 1.0
        // finalScore ≈ 0.55 * 1.0 + 0.30 * ~1.0 + 0.15 * 1.0 ≈ 1.0
        assertEquals(1.0, rec.priceScore());
        assertEquals(1.0, rec.leadTimeScore());
        assertTrue(rec.vendorScore() > 0.99, "Vendor with 1000 evals and score 10 should be near 1.0");
        assertTrue(rec.recommendationScore() > 0.99, "All perfect scores should yield near 1.0");
    }

    @Test
    void rankQuotes_veryBadQuote_returnsLessCompetitiveReason() {
        VendorDto bestVendor = vendor(9.0, 100);
        VendorDto poorVendor = vendor(1.0, 100);
        QuoteDto best = quote(BigDecimal.valueOf(100), 2, bestVendor);
        QuoteDto veryBad = quote(BigDecimal.valueOf(1000), 20, poorVendor);

        List<RecommendedQuoteDto> result = service.rankQuotes(List.of(best, veryBad), 5.0);

        RecommendedQuoteDto nonTop = result.stream()
                .filter(r -> r.quote().totalAmountEuro().compareTo(BigDecimal.valueOf(1000)) == 0)
                .findFirst().orElseThrow();

        assertEquals("Less competitive overall.", nonTop.recommendationReason());
    }
}
