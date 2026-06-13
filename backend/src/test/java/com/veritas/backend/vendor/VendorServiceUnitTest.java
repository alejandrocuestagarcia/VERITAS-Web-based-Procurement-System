package com.veritas.backend.vendor;

import com.veritas.backend.requisition.entity.Invoice;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.dto.VendorEditDto;
import com.veritas.backend.vendor.dto.VendorRatingDto;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.entity.VendorEvaluation;
import com.veritas.backend.vendor.mapper.VendorMapper;
import com.veritas.backend.vendor.repository.VendorEvaluationRepository;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.vendor.service.impl.VendorServiceImpl;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VendorServiceUnitTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private VendorMapper vendorMapper;

    @Mock
    private VendorEvaluationRepository vendorEvaluationRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private VendorServiceImpl vendorService;

    @Test
    void CreateVendor_ValidInput_SavesAndReturnsVendor() {
        VendorDto inputDto = new VendorDto(
                1L,
                "Test Vendor",
                "TAX-123",
                null, null, null, null, null,
                "A test vendor description",
                "John Doe",
                "john@example.com",
                null, null, null
        );

        Vendor mappedVendor = new Vendor();
        mappedVendor.setVendorName(inputDto.vendorName());
        mappedVendor.setTaxId(inputDto.taxId());

        Vendor savedVendor = new Vendor();
        savedVendor.setId(1L);
        savedVendor.setVendorName(inputDto.vendorName());
        savedVendor.setTaxId(inputDto.taxId());
        savedVendor.setDescription(inputDto.description());
        savedVendor.setPrimaryContactName(inputDto.primaryContactName());
        savedVendor.setPrimaryContactEmail(inputDto.primaryContactEmail());

        when(vendorMapper.toVendor(inputDto)).thenReturn(mappedVendor);
        when(vendorMapper.toVendorDto(savedVendor)).thenReturn(inputDto);
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

        VendorDto dto = new VendorDto(
                1L, "Test Vendor", "TAX-123",
                null, null, null, null, null,
                "A test vendor description",
                "John Doe",
                "john@example.com",
                null, null, null
        );

        when(vendorRepository.findByRating(eq("test"), eq(5.0), any(Pageable.class))).thenReturn(page);
        when(vendorMapper.toVendorDto(vendor)).thenReturn(dto);

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

    // AI-GENERATED
    @Test
    void EditVendor_VendorNotFound_ThrowsEntityNotFoundException() {
        when(vendorRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> vendorService.editVendor(99L, new VendorEditDto(
            "Vendor", "TAX-001", "Description", null, null)))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("Vendor not found with id: 99");

        verify(vendorRepository, never()).save(any(Vendor.class));
    }

    // AI-GENERATED
    @Test
    void EditVendor_TaxIdConflict_ThrowsEntityExistsException() {
        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setVendorName("Current");
        vendor.setTaxId("TAX-001");
        vendor.setDescription("Old desc");

        Vendor existing = new Vendor();
        existing.setId(2L);
        existing.setTaxId("TAX-NEW");

        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));
        when(vendorRepository.findByTaxId("TAX-NEW")).thenReturn(java.util.Optional.of(existing));

        assertThatThrownBy(() -> vendorService.editVendor(1L, new VendorEditDto(
            "Updated", "TAX-NEW", "Updated desc", null, null)))
            .isInstanceOf(EntityExistsException.class)
            .hasMessage("Vendor with tax ID 'TAX-NEW' already exists");

        verify(vendorRepository, never()).save(any(Vendor.class));
    }

    // AI-GENERATED
    @Test
    void EditVendor_TaxIdUnchanged_DoesNotCheckForConflicts() {
        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setVendorName("Current");
        vendor.setTaxId("TAX-001");
        vendor.setDescription("Old desc");

        VendorEditDto edits = new VendorEditDto(
            "Updated", "TAX-001", "Updated desc", null, null);

        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VendorDto dto = new VendorDto(
            1L,
            "Updated",
            "TAX-001",
            null, null, null, null, null,
            "Updated desc",
            null,
            null,
            null, null, null
        );
        when(vendorMapper.toVendorDto(any(Vendor.class))).thenReturn(dto);

        vendorService.editVendor(1L, edits);

        verify(vendorRepository, never()).findByTaxId(any());
    }

    // AI-GENERATED
    @Test
    void EditVendor_ValidUpdates_UpdatesFieldsAndSaves() {
        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setVendorName("Old Vendor");
        vendor.setTaxId("TAX-OLD");
        vendor.setDescription("Old description");
        vendor.setPrimaryContactName("Old Contact");
        vendor.setPrimaryContactEmail("old@vendor.com");

        VendorEditDto edits = new VendorEditDto(
            "New Vendor",
            "TAX-NEW",
            "New description",
            "New Contact",
            "new@vendor.com"
        );

        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));
        when(vendorRepository.findByTaxId("TAX-NEW")).thenReturn(java.util.Optional.empty());
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VendorDto dto = new VendorDto(
            1L,
            "New Vendor",
            "TAX-NEW",
            null, null, null, null, null,
            "New description",
            "New Contact",
            "new@vendor.com",
            null, null, null
        );
        when(vendorMapper.toVendorDto(any(Vendor.class))).thenReturn(dto);

        VendorDto result = vendorService.editVendor(1L, edits);

        assertThat(result.vendorName()).isEqualTo("New Vendor");
        assertThat(result.taxId()).isEqualTo("TAX-NEW");
        assertThat(result.description()).isEqualTo("New description");
        assertThat(result.primaryContactName()).isEqualTo("New Contact");
        assertThat(result.primaryContactEmail()).isEqualTo("new@vendor.com");

        ArgumentCaptor<Vendor> vendorCaptor = ArgumentCaptor.forClass(Vendor.class);
        verify(vendorRepository).save(vendorCaptor.capture());
        Vendor savedVendor = vendorCaptor.getValue();
        assertThat(savedVendor.getVendorName()).isEqualTo("New Vendor");
        assertThat(savedVendor.getTaxId()).isEqualTo("TAX-NEW");
        assertThat(savedVendor.getDescription()).isEqualTo("New description");
        assertThat(savedVendor.getPrimaryContactName()).isEqualTo("New Contact");
        assertThat(savedVendor.getPrimaryContactEmail()).isEqualTo("new@vendor.com");
    }

    // AI-GENERATED
    @Test
    void EditVendor_BlankContactFields_DoNotOverwriteExisting() {
        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setVendorName("Vendor");
        vendor.setTaxId("TAX-001");
        vendor.setDescription("Description");
        vendor.setPrimaryContactName("Existing Contact");
        vendor.setPrimaryContactEmail("existing@vendor.com");

        VendorEditDto edits = new VendorEditDto(
            null,
            null,
            null,
            "  ",
            ""
        );

        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VendorDto dto = new VendorDto(
            1L,
            vendor.getVendorName(),
            vendor.getTaxId(),
            null, null, null, null, null,
            vendor.getDescription(),
            vendor.getPrimaryContactName(),
            vendor.getPrimaryContactEmail(),
            null, null, null
        );
        when(vendorMapper.toVendorDto(any(Vendor.class))).thenReturn(dto);

        vendorService.editVendor(1L, edits);

        assertThat(vendor.getPrimaryContactName()).isEqualTo("Existing Contact");
        assertThat(vendor.getPrimaryContactEmail()).isEqualTo("existing@vendor.com");
    }

    @Test
    void RateVendor_ExactQuoteAndInvoiceAmount_SavesWithGapScoreTen() {
        Vendor vendor = new Vendor();
        vendor.setId(1L);

        Request request = new Request();
        request.setRequestID(10L);

        Quote quote = new Quote();
        quote.setSelected(true);
        quote.setTotalAmount(new BigDecimal("150.00"));
        request.setQuotes(List.of(quote));

        Invoice invoice = new Invoice();
        invoice.setTotalAmount(new BigDecimal("150.00"));
        request.setInvoice(invoice);

        User evaluator = new User();
        evaluator.setId(5L);

        VendorRatingDto ratingDto = new VendorRatingDto(9, 8, 9, "Nice performance");

        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));
        when(requestRepository.findById(10L)).thenReturn(java.util.Optional.of(request));
        when(vendorEvaluationRepository.existsByVendorIdAndRequestRequestID(1L, 10L)).thenReturn(false);
        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));
        
        VendorDto returnDto = new VendorDto(1L, "VendorName", "TAX-ID", 9.0, 8.0, 9.0, 10.0, 9.0, "Desc", "Contact", "email@test.com", null, null, null);
        when(vendorMapper.toVendorDto(any(Vendor.class))).thenReturn(returnDto);

        VendorDto result = vendorService.rateVendor(1L, 10L, ratingDto, evaluator);

        assertThat(result).isNotNull();
        ArgumentCaptor<VendorEvaluation> evaluationCaptor = ArgumentCaptor.forClass(VendorEvaluation.class);
        verify(vendorEvaluationRepository).save(evaluationCaptor.capture());
        VendorEvaluation savedEval = evaluationCaptor.getValue();
        assertThat(savedEval.getGapScore()).isEqualTo(10.0);
        assertThat(savedEval.getCommunicationScore()).isEqualTo(9);
        assertThat(savedEval.getDeliveryScore()).isEqualTo(8);
        assertThat(savedEval.getQualityScore()).isEqualTo(9);
    }

    @Test
    void RateVendor_InvoiceExceedsQuote_SavesWithReducedGapScore() {
        Vendor vendor = new Vendor();
        vendor.setId(1L);

        Request request = new Request();
        request.setRequestID(10L);

        Quote quote = new Quote();
        quote.setSelected(true);
        quote.setTotalAmount(new BigDecimal("100.00"));
        request.setQuotes(List.of(quote));

        Invoice invoice = new Invoice();
        invoice.setTotalAmount(new BigDecimal("120.00"));
        request.setInvoice(invoice);

        User evaluator = new User();
        evaluator.setId(5L);

        VendorRatingDto ratingDto = new VendorRatingDto(9, 8, 9, "Nice performance");

        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));
        when(requestRepository.findById(10L)).thenReturn(java.util.Optional.of(request));
        when(vendorEvaluationRepository.existsByVendorIdAndRequestRequestID(1L, 10L)).thenReturn(false);
        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));
        
        VendorDto returnDto = new VendorDto(1L, "VendorName", "TAX-ID", 9.0, 8.0, 9.0, 8.0, 8.5, "Desc", "Contact", "email@test.com", null, null, null);
        when(vendorMapper.toVendorDto(any(Vendor.class))).thenReturn(returnDto);

        vendorService.rateVendor(1L, 10L, ratingDto, evaluator);

        ArgumentCaptor<VendorEvaluation> evaluationCaptor = ArgumentCaptor.forClass(VendorEvaluation.class);
        verify(vendorEvaluationRepository).save(evaluationCaptor.capture());
        VendorEvaluation savedEval = evaluationCaptor.getValue();
        assertThat(savedEval.getGapScore()).isEqualTo(8.0);
    }

    @Test
    void RateVendor_MissingInvoice_SavesWithDefaultGapScoreTen() {
        Vendor vendor = new Vendor();
        vendor.setId(1L);

        Request request = new Request();
        request.setRequestID(10L);

        User evaluator = new User();
        evaluator.setId(5L);

        VendorRatingDto ratingDto = new VendorRatingDto(9, 8, 9, "Nice performance");

        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));
        when(requestRepository.findById(10L)).thenReturn(java.util.Optional.of(request));
        when(vendorEvaluationRepository.existsByVendorIdAndRequestRequestID(1L, 10L)).thenReturn(false);
        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));
        
        VendorDto returnDto = new VendorDto(1L, "VendorName", "TAX-ID", 9.0, 8.0, 9.0, 10.0, 9.0, "Desc", "Contact", "email@test.com", null, null, null);
        when(vendorMapper.toVendorDto(any(Vendor.class))).thenReturn(returnDto);

        vendorService.rateVendor(1L, 10L, ratingDto, evaluator);

        ArgumentCaptor<VendorEvaluation> evaluationCaptor = ArgumentCaptor.forClass(VendorEvaluation.class);
        verify(vendorEvaluationRepository).save(evaluationCaptor.capture());
        VendorEvaluation savedEval = evaluationCaptor.getValue();
        assertThat(savedEval.getGapScore()).isEqualTo(10.0);
    }
}
