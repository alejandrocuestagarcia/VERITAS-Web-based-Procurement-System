package com.veritas.backend.requisition.service.impl;

import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.integrations.currency.dto.CurrencyConversionResult;
import com.veritas.backend.integrations.currency.service.CurrencyConversionService;
import jakarta.persistence.EntityNotFoundException;
import com.veritas.backend.requisition.dto.QuoteCreateDto;
import com.veritas.backend.requisition.dto.QuoteDto;
import com.veritas.backend.requisition.dto.QuoteLineItemCreateDto;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestItem;
import com.veritas.backend.requisition.repository.RequestItemRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.requisition.service.RequisitionQuoteService;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.entity.QuoteLineItem;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.repository.QuoteLineItemRepository;
import com.veritas.backend.vendor.repository.QuoteRepository;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.vendor.mapper.QuoteMapper;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.veritas.backend.common.exception.WorkflowStateException;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.integrations.currency.entity.Currency;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionQuoteServiceImpl implements RequisitionQuoteService {

    private final RequestRepository requestRepository;
    private final QuoteRepository quoteRepository;
    private final VendorRepository vendorRepository;
    private final QuoteLineItemRepository quoteLineItemRepository;
    private final RequestItemRepository requestItemRepository;
    private final QuoteMapper quoteMapper;
    private final CurrencyConversionService currencyConversionService;
    private final InternalBudgetRepository internalBudgetRepository;
    private final RequisitionServiceImpl requisitionService;

    @Override
    @Transactional(readOnly = true)
    public List<QuoteDto> getQuotesForRequest(Long requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + requestId));

        canRequesterOrProcurementOfficerAccessRequestDetails(request);

        List<Quote> quotes = quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(requestId);
        
        return quotes.stream()
            .map(this::mapToDtoWithEuro)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteDto getQuoteById(Long requestId, Long quoteId) {
        Quote quote = getQuoteForRequest(requestId, quoteId);
            
        return mapToDtoWithEuro(quote);
    }

    private Quote getQuoteForRequest(Long requestId, Long quoteId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + requestId));

        canRequesterOrProcurementOfficerAccessRequestDetails(request);

        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found with id: " + quoteId));

        if (!quote.getRequest().getRequestID().equals(requestId)) {
            throw new EntityNotFoundException("Quote does not belong to this request");
        }

        return quote;
    }

    @Override
    @Transactional
    public QuoteDto createQuoteForRequest(Long requestId, QuoteCreateDto createDto) {
        Request request = requestRepository.findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + requestId));

        canRequesterOrProcurementOfficerAccessRequestDetails(request);
            
        Vendor vendor = vendorRepository.findById(createDto.vendorId())
            .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + createDto.vendorId()));

        validateAmounts(createDto);
            
        Quote quote = new Quote();
        quote.setRequest(request);
        quote.setVendorID(vendor);
        quote.setCurrency(createDto.currency());
        quote.setBaseAmount(createDto.baseAmount());
        quote.setShippingCosts(createDto.shippingCosts());
        quote.setTotalAmount(createDto.totalAmount());
        quote.setShippingTime(createDto.shippingTime());
        quote.setSelected(false);
        
        Quote savedQuote = quoteRepository.save(quote);
        
        if (createDto.items() != null && !createDto.items().isEmpty()) {
            saveQuoteLineItems(createDto.items(), savedQuote);
        }
        
        return mapToDtoWithEuro(savedQuote);
    }

    @Override
    @Transactional
    public QuoteDto updateQuoteForRequest(Long requestId, Long quoteId, QuoteCreateDto updateDto) {
        Quote quote = getQuoteForRequest(requestId, quoteId);
        
        if (!quote.getVendorID().getId().equals(updateDto.vendorId())) {
            Vendor vendor = vendorRepository.findById(updateDto.vendorId())
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + updateDto.vendorId()));
            quote.setVendorID(vendor);
        }

        validateAmounts(updateDto);
        
        BigDecimal oldAmount = quote.getTotalAmount() != null ? quote.getTotalAmount() : BigDecimal.ZERO;
        Currency oldCurrency = quote.getCurrency();
        BigDecimal newAmount = updateDto.totalAmount() != null ? updateDto.totalAmount() : BigDecimal.ZERO;

        quote.setCurrency(updateDto.currency());
        quote.setBaseAmount(updateDto.baseAmount());
        quote.setShippingCosts(updateDto.shippingCosts());
        quote.setTotalAmount(updateDto.totalAmount());
        quote.setShippingTime(updateDto.shippingTime());
        
        Quote updatedQuote = quoteRepository.save(quote);

        Request request = quote.getRequest();
        if (quote.isSelected() && request.getBudget() != null) {
            BigDecimal oldAmountEur = currencyConversionService.convert(oldAmount, oldCurrency).convertedAmount();
            BigDecimal newAmountEur = currencyConversionService.convert(newAmount, updateDto.currency()).convertedAmount();
            BigDecimal difference = newAmountEur.subtract(oldAmountEur);
            
            updateCommittedSpendAndValidate(request.getBudget(), difference);
        }

        List<QuoteLineItem> existingItems = quoteLineItemRepository.findByQuoteQuoteID(quoteId);
        quoteLineItemRepository.deleteAll(existingItems);
        
        if (updateDto.items() != null && !updateDto.items().isEmpty()) {
            saveQuoteLineItems(updateDto.items(), quote);
        }
        
        return mapToDtoWithEuro(updatedQuote);
    }

    private void saveQuoteLineItems(List<QuoteLineItemCreateDto> items, Quote quote) {
        for (QuoteLineItemCreateDto itemDto : items) {
            QuoteLineItem item = new QuoteLineItem();
            item.setQuote(quote);
            item.setProductDescription(itemDto.productDescription());
            item.setQuantity(itemDto.quantity());
            item.setUnitPrice(itemDto.unitPrice());
            item.setSubtotal(itemDto.unitPrice().multiply(java.math.BigDecimal.valueOf(itemDto.quantity())));

            if (itemDto.requestItemId() != null) {
                RequestItem reqItem = requestItemRepository.findById(itemDto.requestItemId())
                        .orElse(null);
                item.setRequestItem(reqItem);
            }

            quoteLineItemRepository.save(item);
        }
    }

    @Override
    @Transactional
    public void deleteQuoteForRequest(Long requestId, Long quoteId) {
        Quote quote = getQuoteForRequest(requestId, quoteId);

        Request request = quote.getRequest();
        if (quote.isSelected() && request.getBudget() != null) {
            BigDecimal amountToSubtract = quote.getTotalAmount() != null ? quote.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal amountToSubtractEur = currencyConversionService.convert(amountToSubtract, quote.getCurrency()).convertedAmount();
            updateCommittedSpend(request.getBudget(), amountToSubtractEur.negate());
        }

        List<QuoteLineItem> existingItems = quoteLineItemRepository.findByQuoteQuoteID(quoteId);
        quoteLineItemRepository.deleteAll(existingItems);
        
        quoteRepository.delete(quote);
    }

    @Override
    @Transactional
    public void selectQuoteForRequest(Long requestId, Long quoteId) {
        Quote quoteToSelect = getQuoteForRequest(requestId, quoteId);
        Request request = quoteToSelect.getRequest();
        
        // Unselect all other quotes for this request
        BigDecimal oldAmountEur = BigDecimal.ZERO;
        List<Quote> otherQuotes = quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(requestId);
        for (Quote quote : otherQuotes) {
            if (quote.isSelected()) {
                BigDecimal oldAmount = quote.getTotalAmount() != null ? quote.getTotalAmount() : BigDecimal.ZERO;
                oldAmountEur = currencyConversionService.convert(oldAmount, quote.getCurrency()).convertedAmount();
            }
            quote.setSelected(false);
            quoteRepository.save(quote);
        }

        quoteToSelect.setSelected(true);
        quoteRepository.save(quoteToSelect);

        BigDecimal newAmount = quoteToSelect.getTotalAmount() != null ? quoteToSelect.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal newAmountEur = currencyConversionService.convert(newAmount, quoteToSelect.getCurrency()).convertedAmount();

        if (request.getBudget() != null) {
            BigDecimal difference = newAmountEur.subtract(oldAmountEur);
            updateCommittedSpendAndValidate(request.getBudget(), difference);
        }
    }

    private void validateAmounts(QuoteCreateDto dto) {
        BigDecimal computedBaseAmount = BigDecimal.ZERO;

        for (QuoteLineItemCreateDto item : dto.items()) {
            BigDecimal subtotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            computedBaseAmount = computedBaseAmount.add(subtotal);
        }

        BigDecimal computedTotalAmount = computedBaseAmount.add(dto.shippingCosts());

        if (dto.baseAmount().compareTo(computedBaseAmount) != 0) {
            throw new IllegalArgumentException("Base amount is incorrect");
        }

        if (dto.totalAmount().compareTo(computedTotalAmount) != 0) {
            throw new IllegalArgumentException("Total amount is incorrect");
        }
    }


    private void updateCommittedSpendAndValidate(InternalBudget startBudget, BigDecimal difference) {
        if (startBudget != null) {
            requisitionService.validateBudget(startBudget, difference, true);
            updateCommittedSpend(startBudget, difference);
        }
    }

    private void updateCommittedSpend(InternalBudget startBudget, BigDecimal difference) {
        InternalBudget budget = startBudget;
        while (budget != null) {
            BigDecimal currentCommitted = budget.getCommittedSpend();
            budget.setCommittedSpend(currentCommitted.add(difference));
            internalBudgetRepository.save(budget);
            budget = budget.getParentBudget();
        }
    }

    private QuoteDto mapToDtoWithEuro(Quote quote) {
        List<QuoteLineItem> items = quoteLineItemRepository.findByQuoteQuoteID(quote.getQuoteID());
        CurrencyConversionResult conversion = null;

        try {
            conversion = currencyConversionService.convert(quote.getTotalAmount(), quote.getCurrency());
        } catch (IllegalArgumentException | EntityNotFoundException exception) {
            log.warn("Could not convert quote {} amount {} {} to EUR: {}", quote.getQuoteID(), quote.getTotalAmount(), quote.getCurrency(), exception.getMessage());
            conversion = null;
        }

        return quoteMapper.toDto(quote, items, conversion != null ? conversion.convertedAmount() : null, conversion != null ? conversion.fetchedAt() : null, conversion != null ? conversion.source() : null);
    }

    private void canRequesterOrProcurementOfficerAccessRequestDetails(Request request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        if (user.getRole() == UserRole.REQUESTER && !request.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not allowed to access this request");
        }

        if (user.getRole() == UserRole.PROCUREMENT_OFFICER) {
            if (request.getTeam() == null || user.getDepartment() == null) {
                throw new AccessDeniedException("Not allowed to access this request");
            }

            if (!request.getTeam().getDepartment().getDepartmentId().equals(user.getDepartment().getDepartmentId())) {
                throw new AccessDeniedException("Not allowed to access this request");
            }
        }
    }
}
