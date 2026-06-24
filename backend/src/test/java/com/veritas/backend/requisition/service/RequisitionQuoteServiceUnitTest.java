package com.veritas.backend.requisition.service;

import com.veritas.backend.integrations.currency.dto.CurrencyConversionResult;
import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.integrations.currency.entity.ExchangeRateSource;
import com.veritas.backend.integrations.currency.service.CurrencyConversionService;
import com.veritas.backend.requisition.dto.QuoteCreateDto;
import com.veritas.backend.requisition.dto.QuoteDto;
import com.veritas.backend.requisition.dto.QuoteLineItemCreateDto;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestItem;
import com.veritas.backend.requisition.repository.RequestItemRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.requisition.service.impl.RequisitionQuoteServiceImpl;
import com.veritas.backend.requisition.service.impl.RequisitionServiceImpl;
import com.veritas.backend.requisition.repository.InvoiceRepository;
import com.veritas.backend.requisition.repository.AttachmentRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.entity.QuoteLineItem;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.repository.QuoteLineItemRepository;
import com.veritas.backend.vendor.repository.QuoteRepository;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.vendor.mapper.QuoteMapper;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.requisition.entity.RequestStatus;
import com.veritas.backend.common.exception.WorkflowStateException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

//AI-GENERATED
class RequisitionQuoteServiceUnitTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private QuoteLineItemRepository quoteLineItemRepository;

    @Mock
    private RequestItemRepository requestItemRepository;

    @Mock
    private QuoteMapper quoteMapper;

    @Mock
    private InternalBudgetRepository internalBudgetRepository;

    @Mock
    private CurrencyConversionService currencyConversionService;

    @Mock
    private RequisitionServiceImpl requisitionService;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @InjectMocks
    private RequisitionQuoteServiceImpl quoteService;

    /** A reusable EUR conversion result (rate = 1, no metadata). */
    private static CurrencyConversionResult eurResult(BigDecimal amount) {
        return new CurrencyConversionResult(amount, BigDecimal.ONE, null, null);
    }

    /** A reusable non-EUR conversion result with a fixed test timestamp/source. */
    private static CurrencyConversionResult foreignResult(BigDecimal converted) {
        return new CurrencyConversionResult(converted, new BigDecimal("1.0870"),
                LocalDateTime.of(2024, 1, 1, 0, 0), ExchangeRateSource.FRANKFURTER);
    }

    @BeforeEach
    void setupSecurity() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.ADMINISTRATOR);

        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, List.of());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        lenient().when(currencyConversionService.convert(any(), any()))
                .thenAnswer(invocation -> {
                    BigDecimal amt = invocation.getArgument(0);
                    return new CurrencyConversionResult(amt != null ? amt : BigDecimal.ZERO, BigDecimal.ONE, LocalDateTime.now(), ExchangeRateSource.FRANKFURTER);
                });
    }

    @AfterEach
    void tearDownSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void GetQuotesForRequest_ValidRequestId_ReturnsQuotesList() {
        Long requestId = 1L;
        Request request = new Request();
        request.setRequestID(requestId);

        Quote quote = new Quote();
        quote.setQuoteID(10L);
        quote.setRequest(request);
        quote.setTotalAmount(BigDecimal.valueOf(100));
        quote.setCurrency(Currency.EUR);

        List<QuoteLineItem> items = new ArrayList<>();
        QuoteDto expectedDto = QuoteDto.builder()
                .quoteId(10L)
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(100))
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(100))
                .totalAmountEuro(BigDecimal.valueOf(100))
                .shippingTime(5)
                .isSelected(false)
                .items(List.of())
                .build();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(requestId)).thenReturn(List.of(quote));
        when(quoteLineItemRepository.findByQuoteQuoteID(10L)).thenReturn(items);
        when(quoteMapper.toDto(eq(quote), eq(items), eq(BigDecimal.valueOf(100)), isNull(), isNull())).thenReturn(expectedDto);
        when(currencyConversionService.convert(BigDecimal.valueOf(100), Currency.EUR)).thenReturn(eurResult(BigDecimal.valueOf(100)));

        List<QuoteDto> result = quoteService.getQuotesForRequest(requestId);

        assertAll(
            () -> assertEquals(1, result.size()),
            () -> assertEquals(10L, result.getFirst().quoteId())
        );
        verify(requestRepository).findById(requestId);
        verify(quoteRepository).findByRequestRequestIDOrderByQuoteIDAsc(requestId);
    }

    @Test
    void GetQuotesForRequest_RequestNotFound_ThrowsEntityNotFoundException() {
        Long requestId = 1L;
        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> quoteService.getQuotesForRequest(requestId));
        assertTrue(ex.getMessage().contains("Request not found"));

        verify(quoteRepository, never()).findByRequestRequestIDOrderByQuoteIDAsc(any());
    }

    @Test
    void GetQuoteById_ValidQuoteId_ReturnsQuoteDto() {
        Long requestId = 1L;
        Long quoteId = 10L;

        Request request = new Request();
        request.setRequestID(requestId);

        Quote quote = new Quote();
        quote.setQuoteID(quoteId);
        quote.setRequest(request);
        quote.setTotalAmount(BigDecimal.valueOf(100));
        quote.setCurrency(Currency.EUR);

        List<QuoteLineItem> items = new ArrayList<>();
        QuoteDto expectedDto = QuoteDto.builder()
                .quoteId(quoteId)
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(100))
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(100))
                .totalAmountEuro(BigDecimal.valueOf(100))
                .shippingTime(5)
                .isSelected(false)
                .items(List.of())
                .build();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quoteLineItemRepository.findByQuoteQuoteID(quoteId)).thenReturn(items);
        when(quoteMapper.toDto(eq(quote), eq(items), eq(BigDecimal.valueOf(100)), isNull(), isNull())).thenReturn(expectedDto);
        when(currencyConversionService.convert(BigDecimal.valueOf(100), Currency.EUR)).thenReturn(eurResult(BigDecimal.valueOf(100)));

        QuoteDto result = quoteService.getQuoteById(requestId, quoteId);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(quoteId, result.quoteId())
        );
    }

    @Test
    void GetQuoteById_QuoteNotFound_ThrowsEntityNotFoundException() {
        Request request = new Request();
        request.setRequestID(1L);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> quoteService.getQuoteById(1L, 99L));
        assertTrue(ex.getMessage().contains("Quote not found"));
    }

    @Test
    void GetQuoteById_QuoteDoesNotBelongToRequest_ThrowsEntityNotFoundException() {
        Long requestId = 1L;
        Long otherRequestId = 2L;
        Long quoteId = 10L;

        Request request = new Request();
        request.setRequestID(otherRequestId);

        Quote quote = new Quote();
        quote.setQuoteID(quoteId);
        quote.setRequest(request);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> quoteService.getQuoteById(requestId, quoteId));
        assertTrue(ex.getMessage().contains("Quote does not belong to this request"));
    }

    @Test
    void CreateQuoteForRequest_ValidInput_SavesAndReturnsQuote() {
        Long requestId = 1L;
        Request request = new Request();
        request.setRequestID(requestId);

        Vendor vendor = new Vendor();
        vendor.setId(2L);

        QuoteCreateDto createDto = QuoteCreateDto.builder()
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(100))
                .shippingCosts(BigDecimal.valueOf(10))
                .totalAmount(BigDecimal.valueOf(110))
                .shippingTime(5)
                .items(List.of(new QuoteLineItemCreateDto("Product A", 2, BigDecimal.valueOf(50), 3L)))
                .build();

        Quote quote = new Quote();
        quote.setQuoteID(10L);
        quote.setRequest(request);
        quote.setVendorID(vendor);
        quote.setTotalAmount(BigDecimal.valueOf(100));
        quote.setCurrency(Currency.EUR);

        RequestItem requestItem = new RequestItem();
        requestItem.setId(3L);

        QuoteDto expectedDto = QuoteDto.builder()
                .quoteId(10L)
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(100))
                .shippingCosts(BigDecimal.valueOf(10))
                .totalAmount(BigDecimal.valueOf(110))
                .totalAmountEuro(BigDecimal.valueOf(100))
                .shippingTime(5)
                .isSelected(false)
                .items(List.of())
                .build();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(vendorRepository.findById(2L)).thenReturn(Optional.of(vendor));
        when(quoteRepository.save(any(Quote.class))).thenReturn(quote);
        when(requestItemRepository.findById(3L)).thenReturn(Optional.of(requestItem));
        when(quoteMapper.toDto(eq(quote), anyList(), any(BigDecimal.class), any(), any())).thenReturn(expectedDto);
        when(currencyConversionService.convert(BigDecimal.valueOf(100), Currency.EUR)).thenReturn(eurResult(BigDecimal.valueOf(100)));

        QuoteDto result = quoteService.createQuoteForRequest(requestId, createDto);

        assertNotNull(result);
        verify(quoteRepository).save(any(Quote.class));
        verify(quoteLineItemRepository).save(any(QuoteLineItem.class));
    }

    @Test
    void CreateQuoteForRequest_RequestNotFound_ThrowsEntityNotFoundException() {
        when(requestRepository.findById(1L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> quoteService.createQuoteForRequest(1L, QuoteCreateDto.builder()
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.ZERO)
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .shippingTime(0)
                .items(List.of())
                .build()));
        assertTrue(ex.getMessage().contains("Request not found"));
    }

    @Test
    void CreateQuoteForRequest_VendorNotFound_ThrowsEntityNotFoundException() {
        Request request = new Request();
        request.setRequestID(1L);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(vendorRepository.findById(2L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> quoteService.createQuoteForRequest(1L, QuoteCreateDto.builder()
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.ZERO)
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .shippingTime(0)
                .items(List.of()).build()));
        assertTrue(ex.getMessage().contains("Vendor not found"));
    }

    @Test
    void UpdateQuoteForRequest_ValidInput_UpdatesAndReturnsQuote() {
        Long requestId = 1L;
        Long quoteId = 10L;

        Request request = new Request();
        request.setRequestID(requestId);

        Vendor oldVendor = new Vendor();
        oldVendor.setId(2L);

        Vendor newVendor = new Vendor();
        newVendor.setId(3L);

        Quote quote = new Quote();
        quote.setQuoteID(quoteId);
        quote.setRequest(request);
        quote.setVendorID(oldVendor);
        quote.setTotalAmount(BigDecimal.valueOf(215));
        quote.setCurrency(Currency.USD);

        QuoteCreateDto updateDto = QuoteCreateDto.builder()
                .vendorId(3L)
                .currency(Currency.USD)
                .baseAmount(BigDecimal.valueOf(200))
                .shippingCosts(BigDecimal.valueOf(15))
                .totalAmount(BigDecimal.valueOf(215))
                .shippingTime(5)
                .items(List.of(new QuoteLineItemCreateDto("Product B", 1, BigDecimal.valueOf(200), null)))
                .build();

        QuoteLineItem existingItem = new QuoteLineItem();
        existingItem.setLineItemId(5L);
        existingItem.setQuote(quote);

        QuoteDto expectedDto = QuoteDto.builder()
                .quoteId(quoteId)
                .vendorId(3L)
                .currency(Currency.USD)
                .baseAmount(BigDecimal.valueOf(200))
                .shippingCosts(BigDecimal.valueOf(15))
                .totalAmount(BigDecimal.valueOf(215))
                .totalAmountEuro(BigDecimal.valueOf(100))
                .shippingTime(5)
                .isSelected(false)
                .items(List.of())
                .build();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(vendorRepository.findById(3L)).thenReturn(Optional.of(newVendor));
        when(quoteRepository.save(quote)).thenReturn(quote);
        when(quoteLineItemRepository.findByQuoteQuoteID(quoteId)).thenReturn(List.of(existingItem));
        when(quoteMapper.toDto(eq(quote), anyList(), any(BigDecimal.class), any(), any())).thenReturn(expectedDto);
        when(currencyConversionService.convert(BigDecimal.valueOf(215), Currency.USD))
                .thenReturn(foreignResult(BigDecimal.valueOf(100)));

        QuoteDto result = quoteService.updateQuoteForRequest(requestId, quoteId, updateDto);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(3L, quote.getVendorID().getId()),
            () -> assertEquals(Currency.USD, quote.getCurrency())
        );
        verify(quoteLineItemRepository).deleteAll(anyList());
        verify(quoteLineItemRepository).save(any(QuoteLineItem.class));
    }

    @Test
    void DeleteQuoteForRequest_ValidInput_DeletesQuoteAndItems() {
        Long requestId = 1L;
        Long quoteId = 10L;

        Request request = new Request();
        request.setRequestID(requestId);

        Quote quote = new Quote();
        quote.setQuoteID(quoteId);
        quote.setRequest(request);

        QuoteLineItem item = new QuoteLineItem();
        item.setLineItemId(5L);
        item.setQuote(quote);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quoteLineItemRepository.findByQuoteQuoteID(quoteId)).thenReturn(List.of(item));

        quoteService.deleteQuoteForRequest(requestId, quoteId);

        verify(quoteLineItemRepository).deleteAll(anyList());
        verify(quoteRepository).delete(quote);
    }

    @Test
    void SelectQuoteForRequest_ValidQuoteId_SelectsQuoteAndUnselectsOthers() {
        Long requestId = 1L;
        Long quoteId = 10L;

        Request request = new Request();
        request.setRequestID(requestId);

        Quote quoteToSelect = new Quote();
        quoteToSelect.setQuoteID(quoteId);
        quoteToSelect.setRequest(request);
        quoteToSelect.setSelected(false);

        Quote otherQuote = new Quote();
        otherQuote.setQuoteID(11L);
        otherQuote.setRequest(request);
        otherQuote.setSelected(true);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quoteToSelect));
        when(quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(requestId)).thenReturn(List.of(quoteToSelect, otherQuote));

        quoteService.selectQuoteForRequest(requestId, quoteId);

        assertAll(
            () -> assertTrue(quoteToSelect.isSelected()),
            () -> assertFalse(otherQuote.isSelected())
        );
        verify(quoteRepository, times(3)).save(any(Quote.class)); // 2 for otherQuotes loop + 1 for quoteToSelect save
    }

    @Test
    void GetQuotesForRequest_ConversionSuccess_MapsEuroValue() {
        Long requestId = 1L;
        Request request = new Request();
        request.setRequestID(requestId);

        Quote quote = new Quote();
        quote.setQuoteID(10L);
        quote.setRequest(request);
        quote.setTotalAmount(BigDecimal.valueOf(200));
        quote.setCurrency(Currency.USD);

        List<QuoteLineItem> items = List.of(new QuoteLineItem());

        LocalDateTime fetchedAt = LocalDateTime.of(2024, 1, 1, 0, 0);
        CurrencyConversionResult conversionResult = new CurrencyConversionResult(
                BigDecimal.valueOf(180), new BigDecimal("1.0870"), fetchedAt, ExchangeRateSource.FRANKFURTER);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(requestId)).thenReturn(List.of(quote));
        when(quoteLineItemRepository.findByQuoteQuoteID(10L)).thenReturn(items);
        when(currencyConversionService.convert(BigDecimal.valueOf(200), Currency.USD)).thenReturn(conversionResult);

        QuoteDto expectedDto = QuoteDto.builder()
                .quoteId(10L)
                .vendorId(2L)
                .currency(Currency.USD)
                .baseAmount(BigDecimal.valueOf(200))
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(200))
                .totalAmountEuro(BigDecimal.valueOf(180))
                .exchangeRateFetchedAt(fetchedAt)
                .exchangeRateSource(ExchangeRateSource.FRANKFURTER)
                .shippingTime(5)
                .isSelected(false)
                .items(List.of())
                .build();

        when(quoteMapper.toDto(eq(quote), eq(items), eq(BigDecimal.valueOf(180)),
                eq(fetchedAt), eq(ExchangeRateSource.FRANKFURTER))).thenReturn(expectedDto);

        List<QuoteDto> result = quoteService.getQuotesForRequest(requestId);

        assertAll(
            () -> assertEquals(1, result.size()),
            () -> assertEquals(0, result.getFirst().totalAmountEuro().compareTo(BigDecimal.valueOf(180)))
        );
    }

    @Test
    void GetQuotesForRequest_ConversionIllegalArgument_ReturnsNullEuro() {
        Long requestId = 1L;
        Request request = new Request();
        request.setRequestID(requestId);

        Quote quote = new Quote();
        quote.setQuoteID(10L);
        quote.setRequest(request);
        quote.setTotalAmount(BigDecimal.valueOf(200));
        quote.setCurrency(Currency.USD);

        List<QuoteLineItem> items = List.of();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(requestId)).thenReturn(List.of(quote));
        when(quoteLineItemRepository.findByQuoteQuoteID(10L)).thenReturn(items);

        when(currencyConversionService.convert(any(), any())).thenThrow(new IllegalArgumentException("Illegal input"));

        QuoteDto expectedDto = QuoteDto.builder()
                .quoteId(10L)
                .vendorId(2L)
                .currency(Currency.USD)
                .baseAmount(BigDecimal.valueOf(200))
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(200))
                .totalAmountEuro(null)
                .shippingTime(5)
                .isSelected(false)
                .items(List.of())
                .build();

        when(quoteMapper.toDto(eq(quote), eq(items), isNull(), isNull(), isNull())).thenReturn(expectedDto);

        List<QuoteDto> result = quoteService.getQuotesForRequest(requestId);

        assertNull(result.getFirst().totalAmountEuro());
    }

    //AI-Generated
    @Test
    void SelectQuoteForRequest_UpdatesBudgetCommittedSpend() {
        Long requestId = 1L;
        Long quoteId = 10L;

        // Setup budget hierarchy: Request Budget -> Project Budget
        InternalBudget projectBudget = new InternalBudget();
        projectBudget.setBudgetType(BudgetType.PROJECT);
        projectBudget.setTotalAmount(BigDecimal.valueOf(1000));
        projectBudget.setCommittedSpend(BigDecimal.valueOf(100));

        InternalBudget requestBudget = new InternalBudget();
        requestBudget.setBudgetType(BudgetType.REQUEST);
        requestBudget.setTotalAmount(BigDecimal.ZERO);
        requestBudget.setCommittedSpend(BigDecimal.valueOf(100));
        requestBudget.setParentBudget(projectBudget);

        Request request = new Request();
        request.setRequestID(requestId);
        request.setBudget(requestBudget);

        Quote quoteToSelect = new Quote();
        quoteToSelect.setQuoteID(quoteId);
        quoteToSelect.setRequest(request);
        quoteToSelect.setSelected(false);
        quoteToSelect.setTotalAmount(BigDecimal.valueOf(150));

        Quote otherQuote = new Quote();
        otherQuote.setQuoteID(11L);
        otherQuote.setRequest(request);
        otherQuote.setSelected(true);
        otherQuote.setTotalAmount(BigDecimal.valueOf(100));

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quoteToSelect));
        when(quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(requestId)).thenReturn(List.of(quoteToSelect, otherQuote));

        quoteService.selectQuoteForRequest(requestId, quoteId);

        assertAll("Quote selection committed spend updates",
                () -> assertTrue(quoteToSelect.isSelected()),
                () -> assertFalse(otherQuote.isSelected()),
                // Difference is 150 - 100 = 50. So committedSpend should go from 100 to 150.
                () -> assertEquals(0, requestBudget.getCommittedSpend().compareTo(BigDecimal.valueOf(150))),
                () -> assertEquals(0, projectBudget.getCommittedSpend().compareTo(BigDecimal.valueOf(150)))
        );

        verify(internalBudgetRepository, times(2)).save(any(InternalBudget.class));
        verify(quoteRepository, times(3)).save(any(Quote.class));
    }

    @Test
    void GetQuotesForRequest_MismatchedRequester_ThrowsAccessDeniedException() {
        Long requestId = 1L;
        User creator = new User();
        creator.setId(99L);
        Request request = new Request();
        request.setRequestID(requestId);
        request.setUser(creator);

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.REQUESTER);
        Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(AccessDeniedException.class, () ->
            quoteService.getQuotesForRequest(requestId)
        );
    }

    @Test
    void GetQuotesForRequest_ProcurementOfficerNullTeam_ThrowsAccessDeniedException() {
        Long requestId = 1L;
        Department reqDept = new Department();
        reqDept.setDepartmentId(999L);
        Team team = new Team();
        team.setDepartment(reqDept);
        Request request = new Request();
        request.setRequestID(requestId);
        request.setTeam(team);

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.PROCUREMENT_OFFICER);
        Department officerDept = new Department();
        officerDept.setDepartmentId(1L);
        currentUser.setDepartment(officerDept);
        Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(AccessDeniedException.class, () ->
            quoteService.getQuotesForRequest(requestId)
        );
    }

    @Test
    void GetQuotesForRequest_ProcurementOfficerNullDepartment_ThrowsAccessDeniedException() {
        Long requestId = 1L;
        Department reqDept = new Department();
        reqDept.setDepartmentId(999L);
        Team team = new Team();
        team.setDepartment(reqDept);
        Request request = new Request();
        request.setRequestID(requestId);
        request.setTeam(team);

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.PROCUREMENT_OFFICER);
        currentUser.setDepartment(null);
        Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(AccessDeniedException.class, () ->
            quoteService.getQuotesForRequest(requestId)
        );
    }

    @Test
    void GetQuotesForRequest_ProcurementOfficerDepartmentMismatch_ThrowsAccessDeniedException() {
        Long requestId = 1L;
        Department d1 = new Department();
        d1.setDepartmentId(11L);
        Team team = new Team();
        team.setDepartment(d1);
        Request request = new Request();
        request.setRequestID(requestId);
        request.setTeam(team);

        Department d2 = new Department();
        d2.setDepartmentId(22L);
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.PROCUREMENT_OFFICER);
        currentUser.setDepartment(d2);
        Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(AccessDeniedException.class, () ->
            quoteService.getQuotesForRequest(requestId)
        );
    }

    @Test
    void CreateQuoteForRequest_IncorrectBaseAmount_ThrowsIllegalArgumentException() {
        Long requestId = 1L;
        Request request = new Request();
        request.setRequestID(requestId);

        QuoteCreateDto dto = new QuoteCreateDto(
                2L,
                Currency.EUR,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(110),
                5,
                List.of(
                        new QuoteLineItemCreateDto("Item A", 3, BigDecimal.valueOf(50), null)
                )
        );

        Vendor vendor = new Vendor();
        vendor.setId(2L);
        when(vendorRepository.findById(2L)).thenReturn(Optional.of(vendor));
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            quoteService.createQuoteForRequest(requestId, dto)
        );
        assertEquals("Base amount is incorrect", ex.getMessage());
    }

    @Test
    void CreateQuoteForRequest_IncorrectTotalAmount_ThrowsIllegalArgumentException() {
        Long requestId = 1L;
        Request request = new Request();
        request.setRequestID(requestId);

        QuoteCreateDto dto = new QuoteCreateDto(
                2L,
                Currency.EUR,
                BigDecimal.valueOf(150),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(200),
                5,
                List.of(
                        new QuoteLineItemCreateDto("Item A", 3, BigDecimal.valueOf(50), null)
                )
        );

        Vendor vendor = new Vendor();
        vendor.setId(2L);
        when(vendorRepository.findById(2L)).thenReturn(Optional.of(vendor));
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            quoteService.createQuoteForRequest(requestId, dto)
        );
        assertEquals("Total amount is incorrect", ex.getMessage());
    }

    @Test
    void CreateQuoteForRequest_NullItems_ThrowsNullPointerException() {
        Long requestId = 1L;
        Request request = new Request();
        request.setRequestID(requestId);

        QuoteCreateDto dto = new QuoteCreateDto(
                2L,
                Currency.EUR,
                BigDecimal.valueOf(0),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                5,
                null
        );

        Vendor vendor = new Vendor();
        vendor.setId(2L);
        when(vendorRepository.findById(2L)).thenReturn(Optional.of(vendor));
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.FINANCE_OFFICER);
        Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(NullPointerException.class, () ->
            quoteService.createQuoteForRequest(requestId, dto)
        );
    }

    @Test
    void CreateQuoteForRequest_EmptyItems_SavesQuoteSuccessfully() {
        Long requestId = 1L;
        Request request = new Request();
        request.setRequestID(requestId);

        QuoteCreateDto dto = new QuoteCreateDto(
                2L,
                Currency.EUR,
                BigDecimal.valueOf(0),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                5,
                List.of()
        );

        Vendor vendor = new Vendor();
        vendor.setId(2L);
        when(vendorRepository.findById(2L)).thenReturn(Optional.of(vendor));
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.FINANCE_OFFICER);
        Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Quote savedQuote = new Quote();
        savedQuote.setQuoteID(100L);
        savedQuote.setTotalAmount(BigDecimal.valueOf(10));
        savedQuote.setCurrency(Currency.EUR);
        when(quoteRepository.save(any(Quote.class))).thenReturn(savedQuote);

        QuoteDto expectedDto = QuoteDto.builder()
                .quoteId(100L)
                .build();
        when(quoteMapper.toDto(eq(savedQuote), anyList(), any(), any(), any())).thenReturn(expectedDto);

        QuoteDto result = quoteService.createQuoteForRequest(requestId, dto);
        assertNotNull(result);
        verify(quoteRepository).save(any(Quote.class));
        verify(quoteLineItemRepository, never()).save(any());
    }

    @Test
    void UpdateQuoteForRequest_DifferentVendor_UpdatesVendor() {
        Long requestId = 1L;
        Long quoteId = 10L;

        Request request = new Request();
        request.setRequestID(requestId);

        Vendor oldVendor = new Vendor();
        oldVendor.setId(2L);

        Quote quote = new Quote();
        quote.setQuoteID(quoteId);
        quote.setRequest(request);
        quote.setVendorID(oldVendor);
        quote.setTotalAmount(BigDecimal.valueOf(100));
        quote.setCurrency(Currency.EUR);

        QuoteCreateDto updateDto = new QuoteCreateDto(
                3L,
                Currency.EUR,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(110),
                5,
                List.of(new QuoteLineItemCreateDto("Item A", 2, BigDecimal.valueOf(50), null))
        );

        Vendor newVendor = new Vendor();
        newVendor.setId(3L);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(vendorRepository.findById(3L)).thenReturn(Optional.of(newVendor));
        when(quoteRepository.save(any(Quote.class))).thenReturn(quote);

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.FINANCE_OFFICER);
        Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        QuoteDto expectedDto = QuoteDto.builder().quoteId(quoteId).build();
        when(quoteMapper.toDto(eq(quote), anyList(), any(), any(), any())).thenReturn(expectedDto);

        QuoteDto result = quoteService.updateQuoteForRequest(requestId, quoteId, updateDto);
        assertNotNull(result);
        assertEquals(3L, quote.getVendorID().getId());
    }

    @Test
    void UpdateQuoteForRequest_NullAmounts_ThrowsNullPointerException() {
        Long requestId = 1L;
        Long quoteId = 10L;

        Request request = new Request();
        request.setRequestID(requestId);

        InternalBudget budget = new InternalBudget();
        budget.setCommittedSpend(BigDecimal.ZERO);
        request.setBudget(budget);

        Vendor vendor = new Vendor();
        vendor.setId(2L);

        Quote quote = new Quote();
        quote.setQuoteID(quoteId);
        quote.setRequest(request);
        quote.setVendorID(vendor);
        quote.setTotalAmount(null);
        quote.setCurrency(Currency.EUR);
        quote.setSelected(true);

        QuoteCreateDto updateDto = new QuoteCreateDto(
                2L,
                Currency.EUR,
                BigDecimal.valueOf(0),
                BigDecimal.valueOf(0),
                null,
                5,
                List.of()
        );

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.FINANCE_OFFICER);
        Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(NullPointerException.class, () ->
            quoteService.updateQuoteForRequest(requestId, quoteId, updateDto)
        );
    }


    @Test
    void UpdateQuoteForRequest_LineItemRequestItemNotFound_SetsRequestItemNull() {
        Long requestId = 1L;
        Long quoteId = 10L;

        Request request = new Request();
        request.setRequestID(requestId);

        Vendor vendor = new Vendor();
        vendor.setId(2L);

        Quote quote = new Quote();
        quote.setQuoteID(quoteId);
        quote.setRequest(request);
        quote.setVendorID(vendor);
        quote.setTotalAmount(BigDecimal.valueOf(100));
        quote.setCurrency(Currency.EUR);

        QuoteCreateDto updateDto = new QuoteCreateDto(
                2L,
                Currency.EUR,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(110),
                5,
                List.of(new QuoteLineItemCreateDto("Item A", 2, BigDecimal.valueOf(50), 999L))
        );

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any(Quote.class))).thenReturn(quote);
        when(requestItemRepository.findById(999L)).thenReturn(Optional.empty());

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.FINANCE_OFFICER);
        Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        QuoteDto expectedDto = QuoteDto.builder().quoteId(quoteId).build();
        when(quoteMapper.toDto(eq(quote), anyList(), any(), any(), any())).thenReturn(expectedDto);

        QuoteDto result = quoteService.updateQuoteForRequest(requestId, quoteId, updateDto);
        assertNotNull(result);
        verify(quoteLineItemRepository).save(argThat(item -> item.getRequestItem() == null));
    }


    @Test
    void CanAccess_FinanceOfficer_AllowsAccess() {
        Long requestId = 1L;
        Request request = new Request();
        request.setRequestID(requestId);

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.FINANCE_OFFICER);
        Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(requestId)).thenReturn(List.of());

        List<QuoteDto> result = quoteService.getQuotesForRequest(requestId);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void UpdateQuoteForRequest_EmptyItems_DeletesOldAndDoesNotSaveNew() {
        Long requestId = 1L;
        Long quoteId = 10L;

        Request request = new Request();
        request.setRequestID(requestId);

        Vendor vendor = new Vendor();
        vendor.setId(2L);

        Quote quote = new Quote();
        quote.setQuoteID(quoteId);
        quote.setRequest(request);
        quote.setVendorID(vendor);
        quote.setTotalAmount(BigDecimal.valueOf(100));
        quote.setCurrency(Currency.EUR);

        QuoteCreateDto updateDto = new QuoteCreateDto(
                2L,
                Currency.EUR,
                BigDecimal.valueOf(0),
                BigDecimal.valueOf(0),
                BigDecimal.valueOf(0),
                5,
                List.of()
        );

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any(Quote.class))).thenReturn(quote);

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.FINANCE_OFFICER);
        Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        QuoteDto expectedDto = QuoteDto.builder().quoteId(quoteId).build();
        when(quoteMapper.toDto(eq(quote), anyList(), any(), any(), any())).thenReturn(expectedDto);

        QuoteDto result = quoteService.updateQuoteForRequest(requestId, quoteId, updateDto);
        assertNotNull(result);
        verify(quoteLineItemRepository).deleteAll(anyList());
        verify(quoteLineItemRepository, never()).save(any());
    }

    @Test
    void DeleteQuoteForRequest_NullAmount_DeletesSuccessfully() {
        Long requestId = 1L;
        Long quoteId = 10L;

        Request request = new Request();
        request.setRequestID(requestId);
        request.setBudget(new InternalBudget());

        Quote quote = new Quote();
        quote.setQuoteID(quoteId);
        quote.setRequest(request);
        quote.setSelected(true);
        quote.setTotalAmount(null);
        quote.setCurrency(Currency.EUR);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(currencyConversionService.convert(BigDecimal.ZERO, Currency.EUR)).thenReturn(eurResult(BigDecimal.ZERO));

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.FINANCE_OFFICER);
        Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        quoteService.deleteQuoteForRequest(requestId, quoteId);

        verify(quoteRepository).delete(quote);
    }

    @Test
    void CreateQuoteForRequest_EmptyItemsList_SavesQuoteWithoutLineItems() {
        Long requestId = 1L;
        Request request = new Request();
        request.setRequestID(requestId);

        Vendor vendor = new Vendor();
        vendor.setId(2L);

        QuoteCreateDto createDto = new QuoteCreateDto(
                2L,
                Currency.EUR,
                BigDecimal.ZERO,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                5,
                List.of()
        );

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(vendorRepository.findById(2L)).thenReturn(Optional.of(vendor));

        Quote quote = new Quote();
        quote.setQuoteID(10L);
        quote.setRequest(request);
        quote.setVendorID(vendor);
        quote.setTotalAmount(BigDecimal.valueOf(10));
        quote.setCurrency(Currency.EUR);

        when(quoteRepository.save(any(Quote.class))).thenReturn(quote);

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.FINANCE_OFFICER);
        Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        QuoteDto expectedDto = QuoteDto.builder().quoteId(10L).build();
        when(quoteMapper.toDto(eq(quote), anyList(), any(), any(), any())).thenReturn(expectedDto);

        QuoteDto result = quoteService.createQuoteForRequest(requestId, createDto);
        assertNotNull(result);
        verify(quoteLineItemRepository, never()).save(any());
    }

    @Test
    void UpdateQuoteForRequest_ExistingAmountNull_UsesZero() {
        Long requestId = 1L;
        Long quoteId = 10L;

        Request request = new Request();
        request.setRequestID(requestId);
        request.setBudget(new InternalBudget());

        Vendor vendor = new Vendor();
        vendor.setId(2L);

        Quote quote = new Quote();
        quote.setQuoteID(quoteId);
        quote.setRequest(request);
        quote.setVendorID(vendor);
        quote.setTotalAmount(null);
        quote.setCurrency(Currency.EUR);
        quote.setSelected(true);

        QuoteCreateDto updateDto = new QuoteCreateDto(
                2L,
                Currency.EUR,
                BigDecimal.ZERO,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                5,
                List.of()
        );

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any(Quote.class))).thenReturn(quote);
        when(currencyConversionService.convert(BigDecimal.ZERO, Currency.EUR)).thenReturn(eurResult(BigDecimal.ZERO));
        when(currencyConversionService.convert(BigDecimal.valueOf(10), Currency.EUR)).thenReturn(eurResult(BigDecimal.valueOf(10)));

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.FINANCE_OFFICER);
        Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        QuoteDto expectedDto = QuoteDto.builder().quoteId(quoteId).build();
        when(quoteMapper.toDto(eq(quote), anyList(), any(), any(), any())).thenReturn(expectedDto);

        QuoteDto result = quoteService.updateQuoteForRequest(requestId, quoteId, updateDto);
        assertNotNull(result);
    }

    @Test
    void UpdateQuoteForRequest_NewAmountNull_UsesZero() {
        Long requestId = 1L;
        Long quoteId = 10L;

        Request request = new Request();
        request.setRequestID(requestId);
        request.setBudget(new InternalBudget());

        Vendor vendor = new Vendor();
        vendor.setId(2L);

        Quote quote = new Quote();
        quote.setQuoteID(quoteId);
        quote.setRequest(request);
        quote.setVendorID(vendor);
        quote.setTotalAmount(BigDecimal.valueOf(100));
        quote.setCurrency(Currency.EUR);
        quote.setSelected(true);

        QuoteCreateDto updateDto = new QuoteCreateDto(
                2L,
                Currency.EUR,
                null,
                null,
                null,
                5,
                List.of()
        );

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.FINANCE_OFFICER);
        Authentication auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(NullPointerException.class, () ->
            quoteService.updateQuoteForRequest(requestId, quoteId, updateDto)
        );
    }

    //AI-Generated
    @Test
    void SelectQuoteForRequest_WithInvoice_DeletesInvoiceAndAttachments() {
        Long requestId = 1L;
        Long quoteId = 10L;

        Request request = new Request();
        request.setRequestID(requestId);

        com.veritas.backend.requisition.entity.Invoice invoice = new com.veritas.backend.requisition.entity.Invoice();
        invoice.setInvoiceId(100L);
        com.veritas.backend.requisition.entity.Attachment attachment = new com.veritas.backend.requisition.entity.Attachment();
        attachment.setAttachmentId(200L);
        attachment.setStoragePath("/tmp/nonexistent-test-file-path");
        invoice.setAttachments(new java.util.ArrayList<>(List.of(attachment)));
        request.setInvoice(invoice);

        Quote quoteToSelect = new Quote();
        quoteToSelect.setQuoteID(quoteId);
        quoteToSelect.setRequest(request);
        quoteToSelect.setSelected(false);
        quoteToSelect.setTotalAmount(BigDecimal.valueOf(150));
        quoteToSelect.setCurrency(Currency.EUR);

        Quote currentlySelectedQuote = new Quote();
        currentlySelectedQuote.setQuoteID(11L);
        currentlySelectedQuote.setRequest(request);
        currentlySelectedQuote.setSelected(true);
        currentlySelectedQuote.setTotalAmount(BigDecimal.valueOf(100));
        currentlySelectedQuote.setCurrency(Currency.EUR);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quoteToSelect));
        when(quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(requestId)).thenReturn(List.of(quoteToSelect, currentlySelectedQuote));

        quoteService.selectQuoteForRequest(requestId, quoteId);

        assertAll(
            () -> assertTrue(quoteToSelect.isSelected()),
            () -> assertFalse(currentlySelectedQuote.isSelected()),
            () -> assertNull(request.getInvoice())
        );

        verify(attachmentRepository).delete(attachment);
        verify(invoiceRepository).delete(invoice);
    }

    //AI-Generated
    @Test
    void SelectQuoteForRequest_BudgetValidationFails_DoesNotDeleteInvoiceOrAttachments() {
        Long requestId = 1L;
        Long quoteId = 10L;

        Request request = new Request();
        request.setRequestID(requestId);

        InternalBudget budget = new InternalBudget();
        request.setBudget(budget);

        com.veritas.backend.requisition.entity.Invoice invoice = new com.veritas.backend.requisition.entity.Invoice();
        invoice.setInvoiceId(100L);
        com.veritas.backend.requisition.entity.Attachment attachment = new com.veritas.backend.requisition.entity.Attachment();
        attachment.setAttachmentId(200L);
        attachment.setStoragePath("/tmp/nonexistent-test-file-path");
        invoice.setAttachments(new java.util.ArrayList<>(List.of(attachment)));
        request.setInvoice(invoice);

        Quote quoteToSelect = new Quote();
        quoteToSelect.setQuoteID(quoteId);
        quoteToSelect.setRequest(request);
        quoteToSelect.setSelected(false);
        quoteToSelect.setTotalAmount(BigDecimal.valueOf(150));
        quoteToSelect.setCurrency(Currency.EUR);

        Quote currentlySelectedQuote = new Quote();
        currentlySelectedQuote.setQuoteID(11L);
        currentlySelectedQuote.setRequest(request);
        currentlySelectedQuote.setSelected(true);
        currentlySelectedQuote.setTotalAmount(BigDecimal.valueOf(100));
        currentlySelectedQuote.setCurrency(Currency.EUR);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quoteToSelect));
        when(quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(requestId)).thenReturn(List.of(quoteToSelect, currentlySelectedQuote));

        // Mock validateBudget to throw WorkflowStateException
        doThrow(new com.veritas.backend.common.exception.WorkflowStateException("Budget of : Global budget exhausted including safety buffer."))
                .when(requisitionService).validateBudget(any(), any(), eq(true));

        // Run the call and expect WorkflowStateException
        assertThrows(com.veritas.backend.common.exception.WorkflowStateException.class, () -> {
            quoteService.selectQuoteForRequest(requestId, quoteId);
        });

        // Verify that the invoice was NOT deleted and the request still references it
        assertNotNull(request.getInvoice());
        verifyNoInteractions(attachmentRepository);
        verifyNoInteractions(invoiceRepository);
    }

    //AI-Generated
    @Test
    void CreateQuoteForRequest_RequestFinished_ThrowsWorkflowStateException() {
        Long requestId = 1L;
        Request request = new Request();
        request.setRequestID(requestId);
        request.setState(RequestStatus.FINISHED);

        QuoteCreateDto createDto = QuoteCreateDto.builder()
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(100))
                .shippingCosts(BigDecimal.valueOf(10))
                .totalAmount(BigDecimal.valueOf(110))
                .shippingTime(5)
                .items(List.of())
                .build();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(WorkflowStateException.class, () ->
            quoteService.createQuoteForRequest(requestId, createDto)
        );
    }

    //AI-Generated
    @Test
    void UpdateQuoteForRequest_RequestFinished_ThrowsWorkflowStateException() {
        Long requestId = 1L;
        Long quoteId = 10L;
        Request request = new Request();
        request.setRequestID(requestId);
        request.setState(RequestStatus.FINISHED);

        Quote quote = new Quote();
        quote.setQuoteID(quoteId);
        quote.setRequest(request);

        QuoteCreateDto updateDto = QuoteCreateDto.builder()
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(100))
                .shippingCosts(BigDecimal.valueOf(10))
                .totalAmount(BigDecimal.valueOf(110))
                .shippingTime(5)
                .items(List.of())
                .build();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));

        assertThrows(WorkflowStateException.class, () ->
            quoteService.updateQuoteForRequest(requestId, quoteId, updateDto)
        );
    }

    //AI-Generated
    @Test
    void DeleteQuoteForRequest_RequestFinished_ThrowsWorkflowStateException() {
        Long requestId = 1L;
        Long quoteId = 10L;
        Request request = new Request();
        request.setRequestID(requestId);
        request.setState(RequestStatus.FINISHED);

        Quote quote = new Quote();
        quote.setQuoteID(quoteId);
        quote.setRequest(request);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));

        assertThrows(WorkflowStateException.class, () ->
            quoteService.deleteQuoteForRequest(requestId, quoteId)
        );
    }

    //AI-Generated
    @Test
    void SelectQuoteForRequest_RequestFinished_ThrowsWorkflowStateException() {
        Long requestId = 1L;
        Long quoteId = 10L;
        Request request = new Request();
        request.setRequestID(requestId);
        request.setState(RequestStatus.FINISHED);

        Quote quote = new Quote();
        quote.setQuoteID(quoteId);
        quote.setRequest(request);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));

        assertThrows(WorkflowStateException.class, () ->
            quoteService.selectQuoteForRequest(requestId, quoteId)
        );
    }
}
