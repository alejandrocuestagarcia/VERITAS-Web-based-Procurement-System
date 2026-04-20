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
    void createVendorShouldPersistInDatabase() {
        VendorDto inputDto = new VendorDto(
                "Integration Test Vendor",
                "TAX-INT-456",
                null, null, null, null,
                "Integration test description",
                "Boban Bobanovic",
                "boban@example.com"
        );

        VendorDto result = vendorService.createVendor(inputDto);

        assertThat(result).isNotNull();
        
        Optional<Vendor> persistedVendor = vendorRepository.findByTaxId(inputDto.taxId());
        assertThat(persistedVendor).isPresent();
        assertThat(persistedVendor.get().getVendorName()).isEqualTo(inputDto.vendorName());
        assertThat(persistedVendor.get().getPrimaryContactEmail()).isEqualTo(inputDto.primaryContactEmail());
    }
}
