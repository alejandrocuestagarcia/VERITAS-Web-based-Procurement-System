package com.veritas.backend.requisition.service.impl;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional(readOnly = true)
    public List<QuoteDto> getQuotesForRequest(Long requestId) {
        requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + requestId));
            
        List<Quote> quotes = quoteRepository.findByRequestRequestID(requestId);
        
        return quotes.stream()
            .map(this::mapToDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteDto getQuoteById(Long requestId, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
            .orElseThrow(() -> new EntityNotFoundException("Quote not found with id: " + quoteId));
            
        if (!quote.getRequest().getRequestID().equals(requestId)) {
            throw new EntityNotFoundException("Quote does not belong to this request");
        }
            
        return mapToDto(quote);
    }

    @Override
    @Transactional
    public QuoteDto createQuoteForRequest(Long requestId, QuoteCreateDto createDto) {
        Request request = requestRepository.findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + requestId));
            
        Vendor vendor = vendorRepository.findById(createDto.vendorId())
            .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + createDto.vendorId()));
            
        Quote quote = new Quote();
        quote.setRequest(request);
        quote.setVendorID(vendor);
        quote.setCurrency(createDto.currency());
        quote.setBaseAmount(createDto.baseAmount());
        quote.setShippingCosts(createDto.shippingCosts());
        quote.setTotalAmount(createDto.totalAmount());
        quote.setSelected(false);
        
        Quote savedQuote = quoteRepository.save(quote);
        
        if (createDto.items() != null && !createDto.items().isEmpty()) {
            for (QuoteLineItemCreateDto itemDto : createDto.items()) {
                QuoteLineItem item = new QuoteLineItem();
                item.setQuote(savedQuote);
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
        
        return mapToDto(savedQuote);
    }

    @Override
    @Transactional
    public QuoteDto updateQuoteForRequest(Long requestId, Long quoteId, QuoteCreateDto updateDto) {
        Quote quote = quoteRepository.findById(quoteId)
            .orElseThrow(() -> new EntityNotFoundException("Quote not found with id: " + quoteId));
            
        if (!quote.getRequest().getRequestID().equals(requestId)) {
            throw new EntityNotFoundException("Quote does not belong to this request");
        }
        
        if (!quote.getVendorID().getId().equals(updateDto.vendorId())) {
            Vendor vendor = vendorRepository.findById(updateDto.vendorId())
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + updateDto.vendorId()));
            quote.setVendorID(vendor);
        }
        
        quote.setCurrency(updateDto.currency());
        quote.setBaseAmount(updateDto.baseAmount());
        quote.setShippingCosts(updateDto.shippingCosts());
        quote.setTotalAmount(updateDto.totalAmount());
        
        Quote updatedQuote = quoteRepository.save(quote);
        
        List<QuoteLineItem> existingItems = quoteLineItemRepository.findAll().stream()
            .filter(item -> item.getQuote().getQuoteID().equals(quoteId))
            .toList();
        quoteLineItemRepository.deleteAll(existingItems);
        
        if (updateDto.items() != null && !updateDto.items().isEmpty()) {
            for (QuoteLineItemCreateDto itemDto : updateDto.items()) {
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
        
        return mapToDto(updatedQuote);
    }

    @Override
    @Transactional
    public void deleteQuoteForRequest(Long requestId, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
            .orElseThrow(() -> new EntityNotFoundException("Quote not found with id: " + quoteId));
            
        if (!quote.getRequest().getRequestID().equals(requestId)) {
            throw new EntityNotFoundException("Quote does not belong to this request");
        }
        
        List<QuoteLineItem> existingItems = quoteLineItemRepository.findAll().stream()
            .filter(item -> item.getQuote().getQuoteID().equals(quoteId))
            .toList();
        quoteLineItemRepository.deleteAll(existingItems);
        
        quoteRepository.delete(quote);
    }

    @Override
    @Transactional
    public void selectQuoteForRequest(Long requestId, Long quoteId) {
        Quote quoteToSelect = quoteRepository.findById(quoteId)
            .orElseThrow(() -> new EntityNotFoundException("Quote not found with id: " + quoteId));
            
        if (!quoteToSelect.getRequest().getRequestID().equals(requestId)) {
            throw new EntityNotFoundException("Quote does not belong to this request");
        }
        
        // Unselect all other quotes for this request
        List<Quote> otherQuotes = quoteRepository.findByRequestRequestID(requestId);
        for (Quote quote : otherQuotes) {
            quote.setSelected(false);
            quoteRepository.save(quote);
        }
        
        quoteToSelect.setSelected(true);
        quoteRepository.save(quoteToSelect);
    }

    private QuoteDto mapToDto(Quote quote) {
        List<QuoteLineItem> items = quoteLineItemRepository.findByQuoteQuoteID(quote.getQuoteID());
        return quoteMapper.toDto(quote, items);
    }
}
