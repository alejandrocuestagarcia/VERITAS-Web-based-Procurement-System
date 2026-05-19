package com.veritas.backend.requisition.controller;
 
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;

import com.veritas.backend.config.annotations.IsFinanceOfficer;
import com.veritas.backend.config.annotations.IsProcurementOfficer;
import com.veritas.backend.config.annotations.IsRequester;
import com.veritas.backend.requisition.dto.*;
import com.veritas.backend.requisition.service.RequisitionService;
import com.veritas.backend.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "/requisitions", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Requisition Module", description = "Management of procurement requests")
public class RequisitionController {
    private final RequisitionService requisitionService;

    @Operation(summary = "Create a request", description = "Creates a new procurement request.")
    @PreAuthorize("hasAnyRole('REQUESTER')")
    @PostMapping
    public ResponseEntity<RequisitionDto> createRequest(@Valid @RequestBody RequisitionCreateDto requestBody,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requisitionService.createRequest(requestBody, user));
    }

    @Operation(summary = "Get all requests (Search/Filter)", description = "List requests with filters for status and search terms.")
    @IsRequester
    @GetMapping
    public Page<RequisitionDto> getRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {
        return requisitionService.getRequests(status, search, projectId, user, PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt").nullsLast())));
    }

    @Operation(summary = "Get request details", description = "Returns all details for a single requisition.")
    @IsRequester
    @GetMapping("/{id}")
    public RequisitionDto getRequestById(@PathVariable Long id) {
        return requisitionService.getRequestById(id);
    }

    @Operation(summary = "Update/Edit request", description = "Edit draft details or change the assigned requester.")
    @IsRequester
    @PatchMapping("/{id}")
    public ResponseEntity<RequisitionDto> updateRequest(@PathVariable Long id, @RequestBody RequisitionUpdateDto updates) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Submit request", description = "Finalizes a draft and moves it into the workflow engine.")
    @IsRequester
    @PostMapping("/{id}/submit")
    public ResponseEntity<RequisitionDto> submitRequest(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        RequisitionDto submittedRequest = requisitionService.submitRequest(id, actor);
        return ResponseEntity.ok(submittedRequest);
    }

    @Operation(summary = "Approve request", description = "Moves the request to the next workflow step.")
    @IsRequester
    @PostMapping("/{id}/approve")
    public ResponseEntity<RequisitionDto> approveRequest(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(requisitionService.approveRequest(id, actor));
    }

    @Operation(summary = "Reject request", description = "Rejects the request. Requires a reason in the body.")
    @IsRequester
    @PostMapping("/{id}/reject")
    public ResponseEntity<RequisitionDto> rejectRequest(@PathVariable Long id, @AuthenticationPrincipal User actor, @RequestBody RequisitionRejectDto rejectionData) {
        RequisitionDto updatedRequest = requisitionService.rejectRequest(id, actor, rejectionData);
        return ResponseEntity.ok(updatedRequest);
    }

    @Operation(summary = "Bulk upload quotes", description = "Uploads a CSV file containing multiple vendor quotes for a specific request.")
    @IsRequester
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadQuotes(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        requisitionService.saveAttachment(id, file);
        return ResponseEntity.ok("File " + file.getOriginalFilename() + " uploaded for request " + id);
    }

    @Operation(summary = "Download attachment", description = "Downloads a specific attachment by its ID.")
    @IsRequester
    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        return requisitionService.downloadAttachment(attachmentId);
    }

    @Operation(summary = "Get quote comparison", description = "Returns a side-by-side comparison of quotes, including external market price data.")
    @IsRequester
    @GetMapping("/{id}/comparison")
    public ResponseEntity<QuoteComparisonDto> getQuoteComparison(@PathVariable Long id) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Process final payment", description = "Finalizes a request, marks it as paid, and transitions funds from 'committed' to 'actual' in the budget.")
    @IsFinanceOfficer
    @PostMapping("/{id}/pay")
    public ResponseEntity<RequisitionDto> processPayment(@PathVariable Long id) {
        return ResponseEntity.ok(null);
    }
}
