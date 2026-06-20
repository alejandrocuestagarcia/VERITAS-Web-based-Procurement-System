package com.veritas.backend.vendor.service;

import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.requisition.entity.Invoice;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.repository.InvoiceRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.dto.VendorEditDto;
import com.veritas.backend.vendor.dto.VendorRatingDto;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.repository.QuoteRepository;
import com.veritas.backend.vendor.repository.VendorEvaluationRepository;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.vendor.service.VendorService;
import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VendorServiceIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private VendorService vendorService;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private VendorEvaluationRepository vendorEvaluationRepository;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        clearDatabase();
    }

    @AfterEach
    void cleanUp() {
        clearDatabase();
    }

    private void clearDatabase() {
        jdbcTemplate.update("DELETE FROM vendor_evaluations");
        jdbcTemplate.update("DELETE FROM attachments");
        jdbcTemplate.update("DELETE FROM invoices");
        jdbcTemplate.update("DELETE FROM quotes");
        jdbcTemplate.update("DELETE FROM requests");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM vendors");
    }

    @Test
    void VendorCreation_ValidInput_PersistsInDatabase() {
        VendorDto inputDto = new VendorDto(
                null,
                "Integration Test Vendor",
                "TAX-INT-456",
                null, null, null, null, null,
                "Integration test description",
                "Boban Bobanovic",
                "boban@example.com",
                null, null, null
        );

        VendorDto result = vendorService.createVendor(inputDto);

        assertNotNull(result);
        
        Optional<Vendor> persistedVendor = vendorRepository.findByTaxId(inputDto.taxId());
        assertTrue(persistedVendor.isPresent());
        assertAll(
            () -> assertEquals(inputDto.vendorName(), persistedVendor.get().getVendorName()),
            () -> assertEquals(inputDto.primaryContactEmail(), persistedVendor.get().getPrimaryContactEmail())
        );
    }

    @Test
    void FindVendors_ByStringAndRating_ShowsCorrectVendors() {
        saveVendor("Apple Inc.", "TAX-AAPL");
        saveVendor("Microsoft Corp.", "TAX-MSFT");

        Page<VendorDto> result = vendorService.findVendorsByStringAndRating(PageRequest.of(0, 10), "Apple", 0.0);

        assertAll(
            () -> assertEquals(1, result.getContent().size()),
            () -> assertEquals("Apple Inc.", result.getContent().get(0).vendorName())
        );
    }

    @Test
    void FindVendors_ByStringAndRating_FilteredByTaxId() {
        saveVendor("Apple Inc.", "TAX-AAPL");
        saveVendor("Microsoft Corp.", "TAX-MSFT");

        Page<VendorDto> result = vendorService.findVendorsByStringAndRating(PageRequest.of(0, 10), "MSFT", 0.0);

        assertAll(
            () -> assertEquals(1, result.getContent().size()),
            () -> assertEquals("TAX-MSFT", result.getContent().get(0).taxId())
        );
    }

    // AI-GENERATED
    @Test
    void EditVendor_ValidUpdate_PersistsChanges() {
        Vendor vendor = saveVendor("Original Vendor", "TAX-ORIG");

        VendorEditDto edits = new VendorEditDto(
                "Updated Vendor",
                "TAX-UPDATED",
                "Updated description",
                "Updated Contact",
                "updated@vendor.com"
        );

        VendorDto result = vendorService.editVendor(vendor.getId(), edits);

        assertAll(
            () -> assertEquals("Updated Vendor", result.vendorName()),
            () -> assertEquals("TAX-UPDATED", result.taxId()),
            () -> assertEquals("Updated description", result.description()),
            () -> assertEquals("Updated Contact", result.primaryContactName()),
            () -> assertEquals("updated@vendor.com", result.primaryContactEmail())
        );

        Vendor persisted = vendorRepository.findById(vendor.getId()).orElseThrow();
        assertAll(
            () -> assertEquals("Updated Vendor", persisted.getVendorName()),
            () -> assertEquals("TAX-UPDATED", persisted.getTaxId())
        );
    }

    // AI-GENERATED
    @Test
    void EditVendor_DuplicateTaxId_ThrowsEntityExistsException() {
        Vendor vendor = saveVendor("First Vendor", "TAX-001");
        saveVendor("Second Vendor", "TAX-002");

        VendorEditDto edits = new VendorEditDto(
                "First Vendor",
                "TAX-002",
                "Description",
                null,
                null
        );

        EntityExistsException ex = assertThrows(EntityExistsException.class, () -> vendorService.editVendor(vendor.getId(), edits));
        assertEquals("Vendor with tax ID 'TAX-002' already exists", ex.getMessage());
    }

    private Vendor saveVendor(String name, String taxId) {
        Vendor vendor = new Vendor();
        vendor.setVendorName(name);
        vendor.setTaxId(taxId);
        vendor.setDescription("Test vendors");
        return vendorRepository.save(vendor);
    }

    @Test
    void RateVendor_CalculatesGapAndWeightedOverallScore_PersistsCorrectly() {
        User evaluator = new User();
        evaluator.setEmail("evaluator@veritas.com");
        evaluator.setName("Procurement Officer");
        evaluator.setPasswordHash("secured_pass");
        evaluator.setRole(com.veritas.backend.user.entity.UserRole.PROCUREMENT_OFFICER);
        evaluator.setIsActive(true);
        evaluator = userRepository.save(evaluator);

        Vendor vendor = new Vendor();
        vendor.setVendorName("Reliable Logistics");
        vendor.setTaxId("TAX-REL-789");
        vendor.setDescription("Logistics vendor description");
        vendor = vendorRepository.save(vendor);

        Request request = new Request();
        request.setRequestName("Requisition for Logistics");
        request = requestRepository.save(request);

        Quote quote = new Quote();
        quote.setVendorID(vendor);
        quote.setRequest(request);
        quote.setSelected(true);
        quote.setTotalAmount(new BigDecimal("100.00"));
        quote.setCurrency(Currency.EUR);
        quote.setShippingTime(5);
        quote = quoteRepository.save(quote);

        request.setQuotes(List.of(quote));
        request = requestRepository.save(request);

        Invoice invoice = new Invoice();
        invoice.setVendor(vendor);
        invoice.setRequest(request);
        invoice.setInvoiceNumber("INV-999");
        invoice.setTotalAmount(new BigDecimal("110.00"));
        invoice.setCurrency(Currency.EUR);
        invoice = invoiceRepository.save(invoice);

        request.setInvoice(invoice);
        request = requestRepository.save(request);

        VendorRatingDto ratingData = new VendorRatingDto(8, 9, 8, "Satisfactory performance");
        VendorDto resultDto = vendorService.rateVendor(vendor.getId(), request.getRequestID(), ratingData, evaluator);

        Vendor persistedVendor = vendorRepository.findById(vendor.getId()).orElseThrow();

        assertAll(
            () -> assertNotNull(resultDto),
            () -> assertEquals(9.0, resultDto.gapScore()),
            () -> assertEquals(8.6, resultDto.overallScore()),
            () -> assertEquals(8.0, resultDto.communicationScore()),
            () -> assertEquals(9.0, resultDto.deliveryScore()),
            () -> assertEquals(8.0, resultDto.qualityScore()),
            () -> assertEquals(9.0, persistedVendor.getGapScore()),
            () -> assertEquals(8.6, persistedVendor.getOverallScore())
        );
    }
}
