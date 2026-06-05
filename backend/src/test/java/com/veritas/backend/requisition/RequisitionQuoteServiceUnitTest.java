package com.veritas.backend.requisition;

import com.veritas.backend.integrations.currency.entity.Currency;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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

        List<QuoteLineItem> items = new ArrayList<>();
        QuoteDto expectedDto = new QuoteDto(10L, 2L, null, Currency.EUR, BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.valueOf(100), BigDecimal.valueOf(100), false, List.of());

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(requestId)).thenReturn(List.of(quote));
        when(quoteLineItemRepository.findByQuoteQuoteID(10L)).thenReturn(items);
        when(quoteMapper.toDto(quote, items, BigDecimal.valueOf(100))).thenReturn(expectedDto);

        List<QuoteDto> result = quoteService.getQuotesForRequest(requestId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().quoteId()).isEqualTo(10L);
        verify(requestRepository).findById(requestId);
        verify(quoteRepository).findByRequestRequestIDOrderByQuoteIDAsc(requestId);
    }

    @Test
    void GetQuotesForRequest_RequestNotFound_ThrowsEntityNotFoundException() {
        Long requestId = 1L;
        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.getQuotesForRequest(requestId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Request not found");

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

        List<QuoteLineItem> items = new ArrayList<>();
        QuoteDto expectedDto = new QuoteDto(quoteId, 2L, null, Currency.EUR, BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.valueOf(100), BigDecimal.valueOf(100), false, List.of());

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quoteLineItemRepository.findByQuoteQuoteID(quoteId)).thenReturn(items);
        when(quoteMapper.toDto(quote, items, BigDecimal.valueOf(100))).thenReturn(expectedDto);

        QuoteDto result = quoteService.getQuoteById(requestId, quoteId);

        assertThat(result).isNotNull();
        assertThat(result.quoteId()).isEqualTo(quoteId);
    }

    @Test
    void GetQuoteById_QuoteNotFound_ThrowsEntityNotFoundException() {
        Request request = new Request();
        request.setRequestID(1L);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.getQuoteById(1L, 99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Quote not found");
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

        assertThatThrownBy(() -> quoteService.getQuoteById(requestId, quoteId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Quote does not belong to this request");
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

        RequestItem requestItem = new RequestItem();
        requestItem.setId(3L);

        QuoteDto expectedDto = new QuoteDto(10L, 2L, null, Currency.EUR, BigDecimal.valueOf(100), BigDecimal.valueOf(10), BigDecimal.valueOf(110), BigDecimal.valueOf(100), false, List.of());

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(vendorRepository.findById(2L)).thenReturn(Optional.of(vendor));
        when(quoteRepository.save(any(Quote.class))).thenReturn(quote);
        when(requestItemRepository.findById(3L)).thenReturn(Optional.of(requestItem));
        when(quoteMapper.toDto(eq(quote), anyList(), BigDecimal.valueOf(100))).thenReturn(expectedDto);

        QuoteDto result = quoteService.createQuoteForRequest(requestId, createDto);

        assertThat(result).isNotNull();
        verify(quoteRepository).save(any(Quote.class));
        verify(quoteLineItemRepository).save(any(QuoteLineItem.class));
    }

    @Test
    void CreateQuoteForRequest_RequestNotFound_ThrowsEntityNotFoundException() {
        when(requestRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.createQuoteForRequest(1L, new QuoteCreateDto(2L, Currency.EUR, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of())))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Request not found");
    }

    @Test
    void CreateQuoteForRequest_VendorNotFound_ThrowsEntityNotFoundException() {
        Request request = new Request();
        request.setRequestID(1L);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(vendorRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.createQuoteForRequest(1L, new QuoteCreateDto(2L, Currency.EUR, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of())))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Vendor not found");
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

        QuoteDto expectedDto = new QuoteDto(quoteId, 3L, null, Currency.USD, BigDecimal.valueOf(200), BigDecimal.valueOf(15), BigDecimal.valueOf(215), BigDecimal.valueOf(100), false, List.of());

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(vendorRepository.findById(3L)).thenReturn(Optional.of(newVendor));
        when(quoteRepository.save(quote)).thenReturn(quote);
        when(quoteLineItemRepository.findByQuoteQuoteID(quoteId)).thenReturn(List.of(existingItem));
        when(quoteMapper.toDto(eq(quote), anyList(), BigDecimal.valueOf(100))).thenReturn(expectedDto);

        QuoteDto result = quoteService.updateQuoteForRequest(requestId, quoteId, updateDto);

        assertThat(result).isNotNull();
        assertThat(quote.getVendorID().getId()).isEqualTo(3L);
        assertThat(quote.getCurrency()).isEqualTo(Currency.USD);
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

        assertThat(quoteToSelect.isSelected()).isTrue();
        assertThat(otherQuote.isSelected()).isFalse();
        verify(quoteRepository, times(3)).save(any(Quote.class)); // 2 for otherQuotes loop + 1 for quoteToSelect save
    }
}
