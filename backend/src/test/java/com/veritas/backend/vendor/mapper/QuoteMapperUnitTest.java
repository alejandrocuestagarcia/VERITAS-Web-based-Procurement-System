package com.veritas.backend.vendor.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.veritas.backend.integrations.currency.entity.ExchangeRateSource;
import com.veritas.backend.requisition.dto.QuoteDto;
import com.veritas.backend.requisition.dto.QuoteLineItemDto;
import com.veritas.backend.requisition.entity.RequestItem;
import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.entity.QuoteLineItem;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.integrations.currency.entity.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

class QuoteMapperUnitTest {

    private QuoteMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(QuoteMapper.class);
    }

    @Test
    void toDto_AllNull_ReturnsNull() {
        assertNull(mapper.toDto(null, null, null, null, null));
    }

    @Test
    void toDto_NullQuoteWithOtherValues_MapsOnlyOtherValues() {
        LocalDateTime now = LocalDateTime.now();
        QuoteDto dto = mapper.toDto(null, List.of(), BigDecimal.TEN, now, ExchangeRateSource.FRANKFURTER);
        assertAll(
            () -> assertNull(dto.quoteId()),
            () -> assertEquals(BigDecimal.TEN, dto.totalAmountEuro()),
            () -> assertEquals(now, dto.exchangeRateFetchedAt()),
            () -> assertEquals(ExchangeRateSource.FRANKFURTER, dto.exchangeRateSource()),
            () -> assertTrue(dto.items().isEmpty())
        );
    }

    @Test
    void toDto_WithQuoteAndNullAssociations_MapsCorrectly() {
        Quote quote = new Quote();
        quote.setQuoteID(1L);
        quote.setBaseAmount(BigDecimal.valueOf(100));
        quote.setShippingCosts(BigDecimal.valueOf(10));
        quote.setTotalAmount(BigDecimal.valueOf(110));
        quote.setCurrency(Currency.EUR);
        quote.setSelected(true);
        quote.setVendorID(null);

        QuoteDto dto = mapper.toDto(quote, null, BigDecimal.valueOf(110), null, null);

        assertAll(
            () -> assertEquals(1L, dto.quoteId()),
            () -> assertEquals(BigDecimal.valueOf(100), dto.baseAmount()),
            () -> assertEquals(BigDecimal.valueOf(10), dto.shippingCosts()),
            () -> assertEquals(BigDecimal.valueOf(110), dto.totalAmount()),
            () -> assertEquals(Currency.EUR, dto.currency()),
            () -> assertTrue(dto.isSelected()),
            () -> assertNull(dto.vendorId()),
            () -> assertNull(dto.vendor()),
            () -> assertNull(dto.items())
        );
    }

    @Test
    void toDto_WithAssociations_MapsCorrectly() {
        Vendor vendor = new Vendor();
        vendor.setId(5L);
        vendor.setVendorName("Best Vendor");

        Quote quote = new Quote();
        quote.setQuoteID(1L);
        quote.setBaseAmount(BigDecimal.valueOf(100));
        quote.setShippingCosts(BigDecimal.valueOf(10));
        quote.setTotalAmount(BigDecimal.valueOf(110));
        quote.setCurrency(Currency.USD);
        quote.setSelected(false);
        quote.setVendorID(vendor);

        QuoteLineItem item = new QuoteLineItem();
        item.setLineItemId(2L);
        item.setProductDescription("Item A");
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.valueOf(50));
        item.setSubtotal(BigDecimal.valueOf(100));

        LocalDateTime now = LocalDateTime.now();

        QuoteDto dto = mapper.toDto(quote, List.of(item), BigDecimal.valueOf(100), now, ExchangeRateSource.FRANKFURTER);

        assertAll(
            () -> assertEquals(1L, dto.quoteId()),
            () -> assertEquals(5L, dto.vendorId()),
            () -> assertEquals("Best Vendor", dto.vendor().vendorName()),
            () -> assertEquals(BigDecimal.valueOf(100), dto.baseAmount()),
            () -> assertEquals(BigDecimal.valueOf(10), dto.shippingCosts()),
            () -> assertEquals(BigDecimal.valueOf(110), dto.totalAmount()),
            () -> assertEquals(Currency.USD, dto.currency()),
            () -> assertFalse(dto.isSelected()),
            () -> assertEquals(1, dto.items().size()),
            () -> assertEquals(BigDecimal.valueOf(100), dto.totalAmountEuro()),
            () -> assertEquals(now, dto.exchangeRateFetchedAt()),
            () -> assertEquals(ExchangeRateSource.FRANKFURTER, dto.exchangeRateSource())
        );
    }

    @Test
    void toLineItemDto_NullItem_ReturnsNull() {
        assertNull(mapper.toLineItemDto(null));
    }

    @Test
    void toLineItemDto_NullRequestItem_MapsNullId() {
        QuoteLineItem item = new QuoteLineItem();
        item.setLineItemId(1L);
        item.setProductDescription("Item A");
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.valueOf(50));
        item.setSubtotal(BigDecimal.valueOf(100));
        item.setRequestItem(null);

        QuoteLineItemDto dto = mapper.toLineItemDto(item);

        assertAll(
            () -> assertEquals(1L, dto.lineItemId()),
            () -> assertEquals("Item A", dto.productDescription()),
            () -> assertEquals(2, dto.quantity()),
            () -> assertEquals(BigDecimal.valueOf(50), dto.unitPrice()),
            () -> assertEquals(BigDecimal.valueOf(100), dto.subtotal()),
            () -> assertNull(dto.requestItemId())
        );
    }

    @Test
    void toLineItemDto_WithRequestItem_MapsCorrectly() {
        RequestItem requestItem = new RequestItem();
        requestItem.setId(10L);

        QuoteLineItem item = new QuoteLineItem();
        item.setLineItemId(1L);
        item.setProductDescription("Item A");
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.valueOf(50));
        item.setSubtotal(BigDecimal.valueOf(100));
        item.setRequestItem(requestItem);

        QuoteLineItemDto dto = mapper.toLineItemDto(item);

        assertAll(
            () -> assertEquals(1L, dto.lineItemId()),
            () -> assertEquals(10L, dto.requestItemId())
        );
    }

    @Test
    void toVendorDto_NullVendor_ReturnsNull() {
        assertNull(mapper.toVendorDto(null));
    }

    @Test
    void toVendorDto_MapsCorrectly() {
        Vendor vendor = new Vendor();
        vendor.setId(3L);
        vendor.setVendorName("Vendor Corp");

        VendorDto dto = mapper.toVendorDto(vendor);

        assertAll(
            () -> assertEquals(3L, dto.id()),
            () -> assertEquals("Vendor Corp", dto.vendorName())
        );
    }
}
