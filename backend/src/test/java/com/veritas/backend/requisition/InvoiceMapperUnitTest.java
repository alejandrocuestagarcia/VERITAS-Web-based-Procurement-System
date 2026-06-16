package com.veritas.backend.requisition;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.requisition.dto.InvoiceDto;
import com.veritas.backend.requisition.entity.Invoice;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.mapper.InvoiceMapper;
import com.veritas.backend.vendor.entity.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

class InvoiceMapperUnitTest {

    private InvoiceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(InvoiceMapper.class);
    }

    @Test
    void toDto_NullInvoiceAndTotal_ReturnsNull() {
        assertNull(mapper.toDto(null, null));
    }

    @Test
    void toDto_NullInvoiceWithTotal_MapsOnlyTotal() {
        InvoiceDto dto = mapper.toDto(null, BigDecimal.TEN);
        assertAll(
            () -> assertEquals(BigDecimal.TEN, dto.totalAmountEuro()),
            () -> assertNull(dto.invoiceId()),
            () -> assertNull(dto.requestId()),
            () -> assertNull(dto.invoiceNumber()),
            () -> assertNull(dto.invoiceDate()),
            () -> assertNull(dto.totalAmount()),
            () -> assertNull(dto.currency()),
            () -> assertNull(dto.dueDate()),
            () -> assertNull(dto.isPaid()),
            () -> assertNull(dto.vendorId()),
            () -> assertNull(dto.vendorName())
        );
    }

    @Test
    void toDto_NullAssociations_MapsCorrectly() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(1L);
        invoice.setInvoiceNumber("INV-100");
        invoice.setInvoiceDate(LocalDate.of(2026, 6, 1));
        invoice.setTotalAmount(BigDecimal.valueOf(100.0));
        invoice.setCurrency(Currency.EUR);
        invoice.setDueDate(LocalDate.of(2026, 6, 15));
        invoice.setIsPaid(false);
        invoice.setRequest(null);
        invoice.setVendor(null);

        InvoiceDto dto = mapper.toDto(invoice, BigDecimal.valueOf(110.0));

        assertAll(
            () -> assertEquals(1L, dto.invoiceId()),
            () -> assertEquals("INV-100", dto.invoiceNumber()),
            () -> assertEquals(LocalDate.of(2026, 6, 1), dto.invoiceDate()),
            () -> assertEquals(BigDecimal.valueOf(100.0), dto.totalAmount()),
            () -> assertEquals(Currency.EUR, dto.currency()),
            () -> assertEquals(LocalDate.of(2026, 6, 15), dto.dueDate()),
            () -> assertEquals(false, dto.isPaid()),
            () -> assertEquals(BigDecimal.valueOf(110.0), dto.totalAmountEuro()),
            () -> assertNull(dto.requestId()),
            () -> assertNull(dto.vendorId()),
            () -> assertNull(dto.vendorName())
        );
    }

    @Test
    void toDto_WithAssociations_MapsCorrectly() {
        Request request = new Request();
        request.setRequestID(42L);

        Vendor vendor = new Vendor();
        vendor.setId(10L);
        vendor.setVendorName("Vendor Acme");

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(1L);
        invoice.setInvoiceNumber("INV-100");
        invoice.setInvoiceDate(LocalDate.of(2026, 6, 1));
        invoice.setTotalAmount(BigDecimal.valueOf(100.0));
        invoice.setCurrency(Currency.USD);
        invoice.setDueDate(LocalDate.of(2026, 6, 15));
        invoice.setIsPaid(true);
        invoice.setRequest(request);
        invoice.setVendor(vendor);

        InvoiceDto dto = mapper.toDto(invoice, BigDecimal.valueOf(92.5));

        assertAll(
            () -> assertEquals(1L, dto.invoiceId()),
            () -> assertEquals("INV-100", dto.invoiceNumber()),
            () -> assertEquals(LocalDate.of(2026, 6, 1), dto.invoiceDate()),
            () -> assertEquals(BigDecimal.valueOf(100.0), dto.totalAmount()),
            () -> assertEquals(Currency.USD, dto.currency()),
            () -> assertEquals(LocalDate.of(2026, 6, 15), dto.dueDate()),
            () -> assertEquals(true, dto.isPaid()),
            () -> assertEquals(BigDecimal.valueOf(92.5), dto.totalAmountEuro()),
            () -> assertEquals(42L, dto.requestId()),
            () -> assertEquals(10L, dto.vendorId()),
            () -> assertEquals("Vendor Acme", dto.vendorName())
        );
    }
}
