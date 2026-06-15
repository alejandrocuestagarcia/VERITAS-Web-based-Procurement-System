package com.veritas.backend.vendor;

import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.dto.VendorEditDto;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.repository.QuoteRepository;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.vendor.service.VendorService;
import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VendorServiceIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private VendorService vendorService;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    @BeforeEach
    void setUp() {
        quoteRepository.deleteAll();
        vendorRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        vendorRepository.deleteAll(); }

    @Test
    void VendorCreation_ValidInput_PersistsInDatabase() {
        VendorDto inputDto = new VendorDto(
                null,
                "Integration Test Vendor",
                "TAX-INT-456",
                null, null, null, null,
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
}
