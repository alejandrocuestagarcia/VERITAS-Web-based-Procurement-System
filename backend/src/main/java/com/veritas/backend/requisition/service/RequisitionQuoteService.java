package com.veritas.backend.requisition.service;

import com.veritas.backend.requisition.dto.QuoteCreateDto;
import com.veritas.backend.requisition.dto.QuoteDto;
import com.veritas.backend.requisition.dto.RecommendedQuoteDto;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;

public interface RequisitionQuoteService {

    /**
     * Returns all quotes for the given request, including EUR-converted totals where available.
     * Access is restricted based on the authenticated user's role and ownership.
     *
     * @param requestId the ID of the request
     * @return a list of {@link QuoteDto} ordered by quote ID ascending
     * @throws EntityNotFoundException if the request does not exist
     * @throws AccessDeniedException if the user is not permitted to access the request
     */
    List<QuoteDto> getQuotesForRequest(Long requestId);

    /**
     * Returns all quotes for the given request ranked by recommendation score.
     * Each quote is enriched with price, vendor reliability, and lead time scores,
     * a composite recommendation score, a rank, and a human-readable explanation.
     *
     * @param requestId the ID of the request
     * @return a list of {@link RecommendedQuoteDto} sorted by recommendation score descending
     * @throws EntityNotFoundException if the request does not exist
     * @throws AccessDeniedException if the user is not permitted to access the request
     */
    List<RecommendedQuoteDto> getQuoteRecommendations(Long requestId);

    /**
     * Returns a single quote by ID, validated to belong to the given request.
     *
     * @param requestId the ID of the request
     * @param quoteId the ID of the quote
     * @return the matching {@link QuoteDto}
     * @throws EntityNotFoundException if the request or quote does not exist, or the quote does not belong to the request
     * @throws AccessDeniedException if the user is not permitted to access the request
     */
    QuoteDto getQuoteById(Long requestId, Long quoteId);

    /**
     * Creates a new quote for the given request, including optional line items.
     * Base and total amounts are validated against the sum of line item subtotals.
     *
     * @param requestId the ID of the request
     * @param createDto the quote creation payload
     * @return the created {@link QuoteDto}
     * @throws EntityNotFoundException if the request or vendor does not exist
     * @throws IllegalArgumentException if the base or total amount does not match the computed line item totals
     * @throws AccessDeniedException if the user is not permitted to access the request
     */
    QuoteDto createQuoteForRequest(Long requestId, QuoteCreateDto createDto);

    /**
     * Replaces the fields and line items of an existing quote.
     * Existing line items are deleted and recreated from the update payload.
     *
     * @param requestId the ID of the request
     * @param quoteId the ID of the quote to update
     * @param updateDto the updated quote payload
     * @return the updated {@link QuoteDto}
     * @throws EntityNotFoundException if the request, quote, or vendor does not exist
     * @throws IllegalArgumentException if the base or total amount does not match the computed line item totals
     * @throws AccessDeniedException if the user is not permitted to access the request
     */
    QuoteDto updateQuoteForRequest(Long requestId, Long quoteId, QuoteCreateDto updateDto);

    /**
     * Deletes a quote and all its line items.
     *
     * @param requestId the ID of the request
     * @param quoteId the ID of the quote to delete
     * @throws EntityNotFoundException if the request or quote does not exist
     * @throws AccessDeniedException if the user is not permitted to access the request
     */
    void deleteQuoteForRequest(Long requestId, Long quoteId);

    /**
     * Marks a quote as selected and deselects all other quotes for the same request.
     *
     * @param requestId the ID of the request
     * @param quoteId the ID of the quote to select
     * @throws EntityNotFoundException if the request or quote does not exist
     * @throws AccessDeniedException if the user is not permitted to access the request
     */
    void selectQuoteForRequest(Long requestId, Long quoteId);
}
