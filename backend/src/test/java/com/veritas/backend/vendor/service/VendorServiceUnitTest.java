package com.veritas.backend.vendor.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.veritas.backend.requisition.entity.Invoice;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.dto.VendorEditDto;
import com.veritas.backend.vendor.dto.VendorRatingDto;
import com.veritas.backend.vendor.dto.VendorStatsDto;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorServiceUnitTest {

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
                null, null, null, null, null, null,
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

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(inputDto.vendorName(), result.vendorName()),
            () -> assertEquals(inputDto.taxId(), result.taxId()),
            () -> assertEquals(inputDto.description(), result.description()),
            () -> assertEquals(inputDto.primaryContactName(), result.primaryContactName()),
            () -> assertEquals(inputDto.primaryContactEmail(), result.primaryContactEmail())
        );

        ArgumentCaptor<Vendor> vendorCaptor = ArgumentCaptor.forClass(Vendor.class);
        verify(vendorRepository).save(vendorCaptor.capture());
        Vendor capturedVendor = vendorCaptor.getValue();
        assertAll(
            () -> assertEquals(inputDto.vendorName(), capturedVendor.getVendorName()),
            () -> assertEquals(inputDto.taxId(), capturedVendor.getTaxId())
        );
    }

    @Test
    void FindVendors_ByStringAndRating_ShouldCallRepositoryAndMapToDto() {
        Vendor vendor = new Vendor();
        vendor.setVendorName("Test Vendor");
        Page<Vendor> page = new PageImpl<>(List.of(vendor));

        VendorDto dto = new VendorDto(
                1L, "Test Vendor", "TAX-123",
                null, null, null, null, null, null,
                "A test vendor description",
                "John Doe",
                "john@example.com",
                null, null, null
        );

        when(vendorRepository.findByRating(eq("test"), eq(5.0), any(Pageable.class))).thenReturn(page);
        when(vendorMapper.toVendorDto(vendor)).thenReturn(dto);

        Page<VendorDto> result = vendorService.findVendorsByStringAndRating(PageRequest.of(0, 10), "test", 5.0);

        assertAll(
            () -> assertEquals(1, result.getContent().size()),
            () -> assertEquals("Test Vendor", result.getContent().get(0).vendorName())
        );
        verify(vendorRepository).findByRating(eq("test"), eq(5.0), any(Pageable.class));
    }

    @Test
    void FindVendors_ByStringAndRating_WithInvalidRating_ShouldThrowException() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> vendorService.findVendorsByStringAndRating(PageRequest.of(0, 10), "test", 11.0));
        assertEquals("Rating must be between 0.0 and 10.0", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> vendorService.findVendorsByStringAndRating(PageRequest.of(0, 10), "test", -1.0));
        assertEquals("Rating must be between 0.0 and 10.0", ex2.getMessage());
    }

    // AI-GENERATED
    @Test
    void EditVendor_VendorNotFound_ThrowsEntityNotFoundException() {
        when(vendorRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> vendorService.editVendor(99L, new VendorEditDto(
            "Vendor", "TAX-001", "Description", null, null)));
        assertEquals("Vendor not found with id: 99", ex.getMessage());

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

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByTaxId("TAX-NEW")).thenReturn(Optional.of(existing));

        EntityExistsException ex = assertThrows(EntityExistsException.class, () -> vendorService.editVendor(1L, new VendorEditDto(
            "Updated", "TAX-NEW", "Updated desc", null, null)));
        assertEquals("Vendor with tax ID 'TAX-NEW' already exists", ex.getMessage());

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

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VendorDto dto = new VendorDto(
            1L,
            "Updated",
            "TAX-001",
            null, null, null, null, null, null,
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

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByTaxId("TAX-NEW")).thenReturn(Optional.empty());
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VendorDto dto = new VendorDto(
            1L,
            "New Vendor",
            "TAX-NEW",
            null, null, null, null, null, null,
            "New description",
            "New Contact",
            "new@vendor.com",
            null, null, null
        );
        when(vendorMapper.toVendorDto(any(Vendor.class))).thenReturn(dto);

        VendorDto result = vendorService.editVendor(1L, edits);

        assertAll(
            () -> assertEquals("New Vendor", result.vendorName()),
            () -> assertEquals("TAX-NEW", result.taxId()),
            () -> assertEquals("New description", result.description()),
            () -> assertEquals("New Contact", result.primaryContactName()),
            () -> assertEquals("new@vendor.com", result.primaryContactEmail())
        );

        ArgumentCaptor<Vendor> vendorCaptor = ArgumentCaptor.forClass(Vendor.class);
        verify(vendorRepository).save(vendorCaptor.capture());
        Vendor savedVendor = vendorCaptor.getValue();
        assertAll(
            () -> assertEquals("New Vendor", savedVendor.getVendorName()),
            () -> assertEquals("TAX-NEW", savedVendor.getTaxId()),
            () -> assertEquals("New description", savedVendor.getDescription()),
            () -> assertEquals("New Contact", savedVendor.getPrimaryContactName()),
            () -> assertEquals("new@vendor.com", savedVendor.getPrimaryContactEmail())
        );
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

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VendorDto dto = new VendorDto(
            1L,
            vendor.getVendorName(),
            vendor.getTaxId(),
            null, null, null, null, null, null,
            vendor.getDescription(),
            vendor.getPrimaryContactName(),
            vendor.getPrimaryContactEmail(),
            null, null, null
        );
        when(vendorMapper.toVendorDto(any(Vendor.class))).thenReturn(dto);

        vendorService.editVendor(1L, edits);

        assertAll(
            () -> assertEquals("Existing Contact", vendor.getPrimaryContactName()),
            () -> assertEquals("existing@vendor.com", vendor.getPrimaryContactEmail())
        );
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

        VendorRatingDto ratingDto = new VendorRatingDto(9, 8, 9);

        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));
        when(requestRepository.findById(10L)).thenReturn(java.util.Optional.of(request));
        when(vendorEvaluationRepository.existsByVendorIdAndRequestRequestID(1L, 10L)).thenReturn(false);
        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));

        VendorDto returnDto = new VendorDto(1L, "VendorName", "TAX-ID", 9.0, 8.0, 9.0, 10.0, 9.0, null, "Desc", "Contact", "email@test.com", null, null, null);
        when(vendorMapper.toVendorDto(any(Vendor.class))).thenReturn(returnDto);

        VendorDto result = vendorService.rateVendor(1L, 10L, ratingDto, evaluator);

        ArgumentCaptor<VendorEvaluation> evaluationCaptor = ArgumentCaptor.forClass(VendorEvaluation.class);
        verify(vendorEvaluationRepository).save(evaluationCaptor.capture());
        VendorEvaluation savedEval = evaluationCaptor.getValue();

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(10.0, savedEval.getGapScore()),
            () -> assertEquals(9, savedEval.getCommunicationScore()),
            () -> assertEquals(8, savedEval.getDeliveryScore()),
            () -> assertEquals(9, savedEval.getQualityScore())
        );
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

        VendorRatingDto ratingDto = new VendorRatingDto(9, 8, 9);

        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));
        when(requestRepository.findById(10L)).thenReturn(java.util.Optional.of(request));
        when(vendorEvaluationRepository.existsByVendorIdAndRequestRequestID(1L, 10L)).thenReturn(false);
        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));

        VendorDto returnDto = new VendorDto(1L, "VendorName", "TAX-ID", 9.0, 8.0, 9.0, 8.0, 8.5, null, "Desc", "Contact", "email@test.com", null, null, null);
        when(vendorMapper.toVendorDto(any(Vendor.class))).thenReturn(returnDto);

        vendorService.rateVendor(1L, 10L, ratingDto, evaluator);

        ArgumentCaptor<VendorEvaluation> evaluationCaptor = ArgumentCaptor.forClass(VendorEvaluation.class);
        verify(vendorEvaluationRepository).save(evaluationCaptor.capture());
        VendorEvaluation savedEval = evaluationCaptor.getValue();
        assertEquals(8.0, savedEval.getGapScore());
    }

    @Test
    void RateVendor_MissingInvoice_SavesWithDefaultGapScoreTen() {
        Vendor vendor = new Vendor();
        vendor.setId(1L);

        Request request = new Request();
        request.setRequestID(10L);

        User evaluator = new User();
        evaluator.setId(5L);

        VendorRatingDto ratingDto = new VendorRatingDto(9, 8, 9);

        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));
        when(requestRepository.findById(10L)).thenReturn(java.util.Optional.of(request));
        when(vendorEvaluationRepository.existsByVendorIdAndRequestRequestID(1L, 10L)).thenReturn(false);
        when(vendorRepository.findById(1L)).thenReturn(java.util.Optional.of(vendor));

        VendorDto returnDto = new VendorDto(1L, "VendorName", "TAX-ID", 9.0, 8.0, 9.0, 10.0, 9.0, null, "Desc", "Contact", "email@test.com", null, null, null);
        when(vendorMapper.toVendorDto(any(Vendor.class))).thenReturn(returnDto);

        vendorService.rateVendor(1L, 10L, ratingDto, evaluator);

        ArgumentCaptor<VendorEvaluation> evaluationCaptor = ArgumentCaptor.forClass(VendorEvaluation.class);
        verify(vendorEvaluationRepository).save(evaluationCaptor.capture());
        VendorEvaluation savedEval = evaluationCaptor.getValue();
        assertEquals(10.0, savedEval.getGapScore());
    }

    @Test
    void EditVendor_TaxIdConflictsWithSelf_Succeeds() {
        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setTaxId("TAX-001");

        VendorEditDto edits = new VendorEditDto(null, "TAX-001", null, null, null);

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        vendorService.editVendor(1L, edits);

        verify(vendorRepository, never()).findByTaxId(any());
    }

    @Test
    void EditVendor_TaxIdConflictsWithDifferentVendorWithSameId_Succeeds() {
        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setTaxId("TAX-001");

        Vendor existingSameId = new Vendor();
        existingSameId.setId(1L);
        existingSameId.setTaxId("TAX-NEW");

        VendorEditDto edits = new VendorEditDto(null, "TAX-NEW", null, null, null);

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByTaxId("TAX-NEW")).thenReturn(Optional.of(existingSameId));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        vendorService.editVendor(1L, edits);

        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    void FindVendors_MinimumRatingNull_UsesZeroRating() {
        Vendor vendor = new Vendor();
        Page<Vendor> page = new PageImpl<>(List.of(vendor));
        when(vendorRepository.findByRating(eq("test"), eq(0.0), any(Pageable.class))).thenReturn(page);

        vendorService.findVendorsByStringAndRating(PageRequest.of(0, 10), "test", null);

        verify(vendorRepository).findByRating(eq("test"), eq(0.0), any(Pageable.class));
    }

    @Test
    void GetVendorStats_WithAverageRating_ReturnsRoundedRating() {
        when(vendorRepository.count()).thenReturn(5L);
        when(vendorRepository.getAverageOverallScore()).thenReturn(8.756);

        VendorStatsDto stats = vendorService.getVendorStats();

        assertAll(
            () -> assertEquals(5L, stats.total()),
            () -> assertEquals(8.76, stats.averageRating())
        );
    }

    @Test
    void GetVendorStats_WithNullAverageRating_ReturnsZeroRating() {
        when(vendorRepository.count()).thenReturn(5L);
        when(vendorRepository.getAverageOverallScore()).thenReturn(null);

        VendorStatsDto stats = vendorService.getVendorStats();

        assertAll(
            () -> assertEquals(5L, stats.total()),
            () -> assertEquals(0.0, stats.averageRating())
        );
    }

    @Test
    void GetVendorById_Found_ReturnsDto() {
        Vendor vendor = new Vendor();
        vendor.setId(1L);
        VendorDto dto = new VendorDto(1L, "V", "T", null, null, null, null, null, null, null, null, null, null, null, null);

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(vendorMapper.toVendorDto(vendor)).thenReturn(dto);

        VendorDto result = vendorService.getVendorById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
    }

    @Test
    void GetVendorById_NotFound_ThrowsIllegalArgumentException() {
        when(vendorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> vendorService.getVendorById(1L));
    }

    @Test
    void RateVendor_VendorNotFound_ThrowsEntityNotFoundException() {
        when(vendorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> vendorService.rateVendor(1L, 10L, new VendorRatingDto(5, 5, 5), new User()));
    }

    @Test
    void RateVendor_RequestNotFound_ThrowsEntityNotFoundException() {
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(new Vendor()));
        when(requestRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> vendorService.rateVendor(1L, 10L, new VendorRatingDto(5, 5, 5), new User()));
    }

    @Test
    void RateVendor_AlreadyEvaluated_ThrowsEntityExistsException() {
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(new Vendor()));
        when(requestRepository.findById(10L)).thenReturn(Optional.of(new Request()));
        when(vendorEvaluationRepository.existsByVendorIdAndRequestRequestID(1L, 10L)).thenReturn(true);

        assertThrows(EntityExistsException.class, () -> vendorService.rateVendor(1L, 10L, new VendorRatingDto(5, 5, 5), new User()));
    }

    @Test
    void RateVendor_NullQuoteTotal_SavesWithGapScoreTen() {
        Vendor vendor = new Vendor();
        Request request = new Request();
        request.setInvoice(new Invoice());

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(vendorEvaluationRepository.existsByVendorIdAndRequestRequestID(1L, 10L)).thenReturn(false);

        vendorService.rateVendor(1L, 10L, new VendorRatingDto(5, 5, 5), new User());

        ArgumentCaptor<VendorEvaluation> evaluationCaptor = ArgumentCaptor.forClass(VendorEvaluation.class);
        verify(vendorEvaluationRepository).save(evaluationCaptor.capture());
        assertEquals(10.0, evaluationCaptor.getValue().getGapScore());
    }

    @Test
    void RateVendor_ZeroQuoteTotal_SavesWithGapScoreTen() {
        Vendor vendor = new Vendor();
        Request request = new Request();
        request.setInvoice(new Invoice());
        request.getInvoice().setTotalAmount(new BigDecimal("100.00"));

        Quote quote = new Quote();
        quote.setSelected(true);
        quote.setTotalAmount(BigDecimal.ZERO);
        request.setQuotes(List.of(quote));

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(vendorEvaluationRepository.existsByVendorIdAndRequestRequestID(1L, 10L)).thenReturn(false);

        vendorService.rateVendor(1L, 10L, new VendorRatingDto(5, 5, 5), new User());

        ArgumentCaptor<VendorEvaluation> evaluationCaptor = ArgumentCaptor.forClass(VendorEvaluation.class);
        verify(vendorEvaluationRepository).save(evaluationCaptor.capture());
        assertEquals(10.0, evaluationCaptor.getValue().getGapScore());
    }

    @Test
    void RateVendor_HugeDeviation_SavesWithGapScoreZero() {
        Vendor vendor = new Vendor();
        Request request = new Request();
        request.setInvoice(new Invoice());
        request.getInvoice().setTotalAmount(new BigDecimal("300.00")); // 200% deviation

        Quote quote = new Quote();
        quote.setSelected(true);
        quote.setTotalAmount(new BigDecimal("100.00"));
        request.setQuotes(List.of(quote));

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(vendorEvaluationRepository.existsByVendorIdAndRequestRequestID(1L, 10L)).thenReturn(false);

        vendorService.rateVendor(1L, 10L, new VendorRatingDto(5, 5, 5), new User());

        ArgumentCaptor<VendorEvaluation> evaluationCaptor = ArgumentCaptor.forClass(VendorEvaluation.class);
        verify(vendorEvaluationRepository).save(evaluationCaptor.capture());
        assertEquals(0.0, evaluationCaptor.getValue().getGapScore());
    }
}
