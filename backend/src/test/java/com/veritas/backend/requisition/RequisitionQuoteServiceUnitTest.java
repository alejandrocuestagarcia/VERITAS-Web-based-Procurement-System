package com.veritas.backend.requisition;

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

    @InjectMocks
    private RequisitionQuoteServiceImpl quoteService;

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
        LocalDateTime fixedTime = LocalDateTime.of(2026, 6, 12, 19, 57);

        Request request = new Request();
        request.setRequestID(requestId);

        Quote quote = new Quote();
        quote.setQuoteID(10L);
        quote.setRequest(request);
        quote.setTotalAmount(BigDecimal.valueOf(100));
        quote.setCurrency(Currency.EUR);

        List<QuoteLineItem> items = new ArrayList<>();

        QuoteDto expectedDto = new QuoteDto(10L, 2L, null, Currency.EUR, BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.valueOf(100), BigDecimal.valueOf(100), fixedTime, ExchangeRateSource.FRANKFURTER, false, List.of());

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(requestId)).thenReturn(List.of(quote));
        when(quoteLineItemRepository.findByQuoteQuoteID(10L)).thenReturn(items);

        when(currencyConversionService.convert(BigDecimal.valueOf(100), Currency.EUR))
                .thenReturn(new CurrencyConversionResult(BigDecimal.valueOf(100), BigDecimal.valueOf(0.90), fixedTime, ExchangeRateSource.FRANKFURTER));

        when(quoteMapper.toDto(eq(quote), eq(items), eq(BigDecimal.valueOf(100)), any(LocalDateTime.class), eq(ExchangeRateSource.FRANKFURTER)))
                .thenReturn(expectedDto);

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

        LocalDateTime fixedTime = LocalDateTime.of(2026, 6, 12, 19, 57);

        Request request = new Request();
        request.setRequestID(requestId);

        Quote quote = new Quote();
        quote.setQuoteID(quoteId);
        quote.setRequest(request);
        quote.setTotalAmount(BigDecimal.valueOf(100));
        quote.setCurrency(Currency.EUR);

        List<QuoteLineItem> items = new ArrayList<>();

        QuoteDto expectedDto = new QuoteDto(
                quoteId,
                2L,
                null,
                Currency.EUR,
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100),
                fixedTime,
                ExchangeRateSource.FRANKFURTER,
                false,
                List.of()
        );

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quoteLineItemRepository.findByQuoteQuoteID(quoteId)).thenReturn(items);
        when(currencyConversionService.convert(BigDecimal.valueOf(100),Currency.EUR)).thenReturn(
                new CurrencyConversionResult(
                        BigDecimal.valueOf(100),
                        BigDecimal.valueOf(0.90),
                        fixedTime,
                        ExchangeRateSource.FRANKFURTER
                )
        );

        when(quoteMapper.toDto(
                eq(quote),
                eq(items),
                eq(BigDecimal.valueOf(100)),
                any(LocalDateTime.class),
                eq(ExchangeRateSource.FRANKFURTER)
        )).thenReturn(expectedDto);

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

        QuoteCreateDto createDto = new QuoteCreateDto(
                2L,
                Currency.EUR,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(110),
                List.of(new QuoteLineItemCreateDto("Product A", 2, BigDecimal.valueOf(50), 3L))
        );

        Quote quote = new Quote();
        quote.setQuoteID(10L);
        quote.setRequest(request);
        quote.setVendorID(vendor);
        quote.setTotalAmount(BigDecimal.valueOf(100));
        quote.setCurrency(Currency.EUR);

        RequestItem requestItem = new RequestItem();
        requestItem.setId(3L);

        QuoteDto expectedDto = new QuoteDto(10L, 2L, null, Currency.EUR, BigDecimal.valueOf(100), BigDecimal.valueOf(10), BigDecimal.valueOf(110), BigDecimal.valueOf(100),  LocalDateTime.now(), ExchangeRateSource.FRANKFURTER,false, List.of());

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(vendorRepository.findById(2L)).thenReturn(Optional.of(vendor));
        when(quoteRepository.save(any(Quote.class))).thenReturn(quote);
        when(requestItemRepository.findById(3L)).thenReturn(Optional.of(requestItem));
        when(quoteMapper.toDto(eq(quote), anyList(), any(BigDecimal.class), any(LocalDateTime.class), any(ExchangeRateSource.class))).thenReturn(expectedDto);
        when(currencyConversionService.convert(BigDecimal.valueOf(100), Currency.EUR)).thenReturn(new CurrencyConversionResult(BigDecimal.valueOf(100),  BigDecimal.valueOf(0.90), LocalDateTime.now(), ExchangeRateSource.FRANKFURTER));

        QuoteDto result = quoteService.createQuoteForRequest(requestId, createDto);

        assertNotNull(result);
        verify(quoteRepository).save(any(Quote.class));
        verify(quoteLineItemRepository).save(any(QuoteLineItem.class));
    }

    @Test
    void CreateQuoteForRequest_RequestNotFound_ThrowsEntityNotFoundException() {
        when(requestRepository.findById(1L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> quoteService.createQuoteForRequest(1L, new QuoteCreateDto(2L, Currency.EUR, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of())));
        assertTrue(ex.getMessage().contains("Request not found"));
    }

    @Test
    void CreateQuoteForRequest_VendorNotFound_ThrowsEntityNotFoundException() {
        Request request = new Request();
        request.setRequestID(1L);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(vendorRepository.findById(2L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> quoteService.createQuoteForRequest(1L, new QuoteCreateDto(2L, Currency.EUR, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of())));
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

        QuoteCreateDto updateDto = new QuoteCreateDto(
                3L,
                Currency.USD,
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(15),
                BigDecimal.valueOf(215),
                List.of(new QuoteLineItemCreateDto("Product B", 1, BigDecimal.valueOf(200), null))
        );

        QuoteLineItem existingItem = new QuoteLineItem();
        existingItem.setLineItemId(5L);
        existingItem.setQuote(quote);

        QuoteDto expectedDto = new QuoteDto(quoteId, 3L, null, Currency.USD, BigDecimal.valueOf(200), BigDecimal.valueOf(15), BigDecimal.valueOf(215), BigDecimal.valueOf(100),  LocalDateTime.now(), ExchangeRateSource.FRANKFURTER,false, List.of());

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(vendorRepository.findById(3L)).thenReturn(Optional.of(newVendor));
        when(quoteRepository.save(quote)).thenReturn(quote);
        when(quoteLineItemRepository.findByQuoteQuoteID(quoteId)).thenReturn(List.of(existingItem));
        when(quoteMapper.toDto(eq(quote), anyList(), any(BigDecimal.class), any(LocalDateTime.class), any(ExchangeRateSource.class))).thenReturn(expectedDto);
        when(currencyConversionService.convert(BigDecimal.valueOf(215), Currency.USD)).thenReturn(new CurrencyConversionResult(BigDecimal.valueOf(100),  BigDecimal.valueOf(0.90), LocalDateTime.now(), ExchangeRateSource.FRANKFURTER));

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

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(requestId)).thenReturn(List.of(quote));
        when(quoteLineItemRepository.findByQuoteQuoteID(10L)).thenReturn(items);
        when(currencyConversionService.convert(BigDecimal.valueOf(200), Currency.USD)).thenReturn(new CurrencyConversionResult(BigDecimal.valueOf(180),  BigDecimal.valueOf(0.90), LocalDateTime.now(), ExchangeRateSource.FRANKFURTER));

        QuoteDto expectedDto = new QuoteDto(
                10L, 2L, null,
                Currency.USD,
                BigDecimal.valueOf(200),
                BigDecimal.ZERO,
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(180),
                LocalDateTime.now(),
                ExchangeRateSource.FRANKFURTER,
                false,
                List.of()
        );

        when(quoteMapper.toDto(eq(quote), eq(items), eq(BigDecimal.valueOf(180)), any(LocalDateTime.class), any(ExchangeRateSource.class))).thenReturn(expectedDto);

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

        QuoteDto expectedDto = new QuoteDto(
                10L, 2L, null,
                Currency.USD,
                BigDecimal.valueOf(200),
                BigDecimal.ZERO,
                BigDecimal.valueOf(200),
                null,
                LocalDateTime.now(),
                ExchangeRateSource.FRANKFURTER,
                false,
                List.of()
        );

        when(quoteMapper.toDto(eq(quote), eq(items), isNull(), any(), any())).thenReturn(expectedDto);

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
}
