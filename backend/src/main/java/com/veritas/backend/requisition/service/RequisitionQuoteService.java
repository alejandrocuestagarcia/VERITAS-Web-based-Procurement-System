package com.veritas.backend.requisition.service;

import com.veritas.backend.requisition.dto.QuoteCreateDto;
import com.veritas.backend.requisition.dto.QuoteDto;
import java.util.List;

public interface RequisitionQuoteService {
    List<QuoteDto> getQuotesForRequest(Long requestId);
    QuoteDto getQuoteById(Long requestId, Long quoteId);
    QuoteDto createQuoteForRequest(Long requestId, QuoteCreateDto createDto);
    QuoteDto updateQuoteForRequest(Long requestId, Long quoteId, QuoteCreateDto updateDto);
    void deleteQuoteForRequest(Long requestId, Long quoteId);
    void selectQuoteForRequest(Long requestId, Long quoteId);
}
