package com.veritas.backend.vendor;

import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.vendor.service.impl.VendorServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VendorServiceUnitTest {

    @Mock
    private VendorRepository vendorRepository;

    @InjectMocks
    private VendorServiceImpl vendorService;

    @Test
    void CreateVendor_ValidInput_SavesAndReturnsVendor() {
        VendorDto inputDto = new VendorDto(
                "Test Vendor",
                "TAX-123",
                null, null, null, null,
                "A test vendor description",
                "John Doe",
                "john@example.com"
        );

        Vendor savedVendor = new Vendor();
        savedVendor.setId(1L);
        savedVendor.setVendorName(inputDto.vendorName());
        savedVendor.setTaxId(inputDto.taxId());
        savedVendor.setDescription(inputDto.description());
        savedVendor.setPrimaryContactName(inputDto.primaryContactName());
        savedVendor.setPrimaryContactEmail(inputDto.primaryContactEmail());

        when(vendorRepository.save(any(Vendor.class))).thenReturn(savedVendor);

        VendorDto result = vendorService.createVendor(inputDto);

        assertThat(result).isNotNull();
        assertThat(result.vendorName()).isEqualTo(inputDto.vendorName());
        assertThat(result.taxId()).isEqualTo(inputDto.taxId());
        assertThat(result.description()).isEqualTo(inputDto.description());
        assertThat(result.primaryContactName()).isEqualTo(inputDto.primaryContactName());
        assertThat(result.primaryContactEmail()).isEqualTo(inputDto.primaryContactEmail());

        ArgumentCaptor<Vendor> vendorCaptor = ArgumentCaptor.forClass(Vendor.class);
        verify(vendorRepository).save(vendorCaptor.capture());
        Vendor capturedVendor = vendorCaptor.getValue();
        assertThat(capturedVendor.getVendorName()).isEqualTo(inputDto.vendorName());
        assertThat(capturedVendor.getTaxId()).isEqualTo(inputDto.taxId());
    }

    @Test
    void FindVendors_ByStringAndRating_ShouldCallRepositoryAndMapToDto() {
        Vendor vendor = new Vendor();
        vendor.setVendorName("Test Vendor");
        Page<Vendor> page = new PageImpl<>(List.of(vendor));

        when(vendorRepository.findByRating(eq("test"), eq(5.0), any(Pageable.class)))
                .thenReturn(page);

        Page<VendorDto> result = vendorService.findVendorsByStringAndRating(PageRequest.of(0, 10), "test", 5.0);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).vendorName()).isEqualTo("Test Vendor");
        verify(vendorRepository).findByRating(eq("test"), eq(5.0), any(Pageable.class));
    }

    @Test
    void FindVendors_ByStringAndRating_WithInvalidRating_ShouldThrowException() {
        assertThatThrownBy(() -> vendorService.findVendorsByStringAndRating(PageRequest.of(0, 10), "test", 11.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rating must be between 0.0 and 10.0");

        assertThatThrownBy(() -> vendorService.findVendorsByStringAndRating(PageRequest.of(0, 10), "test", -1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rating must be between 0.0 and 10.0");
    }
}
