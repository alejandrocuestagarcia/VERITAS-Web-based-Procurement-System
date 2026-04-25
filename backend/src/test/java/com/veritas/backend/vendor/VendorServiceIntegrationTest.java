package com.veritas.backend.vendor;

import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.vendor.service.VendorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class VendorServiceIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private VendorService vendorService;

    @Autowired
    private VendorRepository vendorRepository;

    @BeforeEach
    void setUp() {
        vendorRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() { vendorRepository.deleteAll(); }

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

        assertThat(result).isNotNull();
        
        Optional<Vendor> persistedVendor = vendorRepository.findByTaxId(inputDto.taxId());
        assertThat(persistedVendor).isPresent();
        assertThat(persistedVendor.get().getVendorName()).isEqualTo(inputDto.vendorName());
        assertThat(persistedVendor.get().getPrimaryContactEmail()).isEqualTo(inputDto.primaryContactEmail());
    }

    @Test
    void FindVendors_ByStringAndRating_ShowsCorrectVendors() {
        saveVendor("Apple Inc.", "TAX-AAPL");
        saveVendor("Microsoft Corp.", "TAX-MSFT");

        Page<VendorDto> result = vendorService.findVendorsByStringAndRating(PageRequest.of(0, 10), "Apple", 0.0);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).vendorName()).isEqualTo("Apple Inc.");
    }

    @Test
    void FindVendors_ByStringAndRating_FilteredByTaxId() {
        saveVendor("Apple Inc.", "TAX-AAPL");
        saveVendor("Microsoft Corp.", "TAX-MSFT");

        Page<VendorDto> result = vendorService.findVendorsByStringAndRating(PageRequest.of(0, 10), "MSFT", 0.0);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).taxId()).isEqualTo("TAX-MSFT");
    }

    private Vendor saveVendor(String name, String taxId) {
        Vendor vendor = new Vendor();
        vendor.setVendorName(name);
        vendor.setTaxId(taxId);
        vendor.setDescription("Test vendors");
        return vendorRepository.save(vendor);
    }
}
