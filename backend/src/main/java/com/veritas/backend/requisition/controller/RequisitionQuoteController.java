package com.veritas.backend.requisition.controller;

import com.veritas.backend.config.annotations.IsProcurementOfficer;
import com.veritas.backend.config.annotations.IsRequester;
import com.veritas.backend.requisition.dto.QuoteCreateDto;
import com.veritas.backend.requisition.dto.QuoteDto;
import com.veritas.backend.requisition.service.RequisitionQuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/requisitions/{requestId}/quotes")
@RequiredArgsConstructor
@Tag(name = "Requisition Quotes Module", description = "Management of quotes for procurement requests")
public class RequisitionQuoteController {

    private final RequisitionQuoteService requisitionQuoteService;

    @Operation(summary = "List quotes for request", description = "Retrieves all quotes associated with a request.")
    @IsRequester
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<QuoteDto>> getQuotesForRequest(@PathVariable Long requestId) {
        log.info("GET /requisitions/{}/quotes", requestId);
        return ResponseEntity.ok(requisitionQuoteService.getQuotesForRequest(requestId));
    }

    @Operation(summary = "Get quote", description = "Retrieves a specific quote.")
    @IsRequester
    @GetMapping(value = "/{quoteId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QuoteDto> getQuote(@PathVariable Long requestId, @PathVariable Long quoteId) {
        log.info("GET /requisitions/{}/quotes/{}", requestId, quoteId);
        return ResponseEntity.ok(requisitionQuoteService.getQuoteById(requestId, quoteId));
    }

    @Operation(summary = "Create quote", description = "Creates a new quote for a request.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @IsProcurementOfficer
    public ResponseEntity<QuoteDto> createQuote(@PathVariable Long requestId, @Valid @RequestBody QuoteCreateDto request) {
        log.info("POST /requisitions/{}/quotes", requestId);
        QuoteDto quoteDto = requisitionQuoteService.createQuoteForRequest(requestId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(quoteDto.quoteId()).toUri();
        return ResponseEntity.created(location).body(quoteDto);
    }

    @Operation(summary = "Update quote", description = "Updates an existing quote.")
    @PutMapping(value = "/{quoteId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @IsProcurementOfficer
    public ResponseEntity<QuoteDto> updateQuote(@PathVariable Long requestId, @PathVariable Long quoteId, @Valid @RequestBody QuoteCreateDto request) {
        log.info("PUT /requisitions/{}/quotes/{}", requestId, quoteId);
        return ResponseEntity.ok(requisitionQuoteService.updateQuoteForRequest(requestId, quoteId, request));
    }

    @Operation(summary = "Delete quote", description = "Deletes an existing quote.")
    @DeleteMapping(value = "/{quoteId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @IsProcurementOfficer
    public ResponseEntity<Void> deleteQuote(@PathVariable Long requestId, @PathVariable Long quoteId) {
        log.info("DELETE /requisitions/{}/quotes/{}", requestId, quoteId);
        requisitionQuoteService.deleteQuoteForRequest(requestId, quoteId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Select quote", description = "Selects a quote as the preferred one for a request.")
    @PostMapping(value = "/{quoteId}/select", produces = MediaType.APPLICATION_JSON_VALUE)
    @IsProcurementOfficer
    public ResponseEntity<Void> selectQuote(@PathVariable Long requestId, @PathVariable Long quoteId) {
        log.info("POST /requisitions/{}/quotes/{}/select", requestId, quoteId);
        requisitionQuoteService.selectQuoteForRequest(requestId, quoteId);
        return ResponseEntity.ok().build();
    }
}
