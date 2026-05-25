package com.veritas.backend.requisition.controller;

import com.veritas.backend.config.annotations.IsFinanceOfficer;
import com.veritas.backend.config.annotations.IsProcurementOfficer;
import com.veritas.backend.config.annotations.IsRequester;
import com.veritas.backend.requisition.dto.*;
import com.veritas.backend.requisition.service.RequisitionService;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.entity.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
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

    @Operation(summary = "Update/Edit request", description = "Edit draft details.")
    @IsRequester
    @PatchMapping("/{id}")
    public ResponseEntity<RequisitionDto> updateRequest(@PathVariable Long id, @Valid @RequestBody RequisitionUpdateDto updates,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(requisitionService.updateRequest(id, updates, user));
    }

    @Operation(summary = "Change requester", description = "Change the assigned requester.")
    @IsFinanceOfficer
    @PatchMapping ("/{requestId}/requester-change")
    public ResponseEntity<RequisitionDto> changeRequester(@PathVariable Long requestId, @RequestBody Long newRequesterId) {
        log.info("PATCH /requisitions/{}/requester-change", requestId);
        return ResponseEntity.ok(requisitionService.changeRequester(requestId, newRequesterId));
    }

    @Operation(summary = "Submit request", description = "Finalizes a draft and moves it into the workflow engine.")
    @IsRequester
    @PostMapping("/{id}/submit")
    public ResponseEntity<RequisitionDto> submitRequest(@PathVariable Long id, @AuthenticationPrincipal User actor, @RequestParam(required = false) Long nextAssigneeId) {
        RequisitionDto submittedRequest = requisitionService.submitRequest(id, actor, nextAssigneeId);
        return ResponseEntity.ok(submittedRequest);
    }

    @Operation(summary = "Approve request", description = "Moves the request to the next workflow step.")
    @IsRequester
    @PostMapping("/{id}/approve")
    public ResponseEntity<RequisitionDto> approveRequest(@PathVariable Long id, @AuthenticationPrincipal User actor, @RequestParam(required = false) Long nextAssigneeId) {
        return ResponseEntity.ok(requisitionService.approveRequest(id, actor, nextAssigneeId));
    }

    @Operation(summary = "Reject request", description = "Rejects the request. Requires a reason in the body.")
    @IsRequester
    @PostMapping("/{id}/reject")
    public ResponseEntity<RequisitionDto> rejectRequest(@PathVariable Long id, @AuthenticationPrincipal User actor, @RequestBody RequisitionRejectDto rejectionData) {
        RequisitionDto updatedRequest = requisitionService.rejectRequest(id, actor, rejectionData);
        return ResponseEntity.ok(updatedRequest);
    }

    @Operation(summary = "Get next step role", description = "Determines the role required for the next workflow step.")
    @IsRequester
    @GetMapping("/{id}/next-step-role")
    public ResponseEntity<String> getNextStepRole(@PathVariable Long id) {
        String role = requisitionService.getNextStepRole(id);
        return ResponseEntity.ok(role != null ? "\"" + role + "\"" : "\"\"");
    }

    @Operation(summary = "Get eligible assignees", description = "Fetches eligible users for the given role and request's department.")
    @IsRequester
    @GetMapping("/{id}/eligible-assignees")
    public ResponseEntity<List<UserDto>> getEligibleAssignees(@PathVariable Long id, @RequestParam String role) {
        return ResponseEntity.ok(requisitionService.getEligibleAssignees(id, role));
    }

    @Operation(summary = "Check if user can act", description = "Checks if the logged-in user can approve or reject the request.")
    @IsRequester
    @GetMapping("/{id}/can-act")
    public ResponseEntity<Boolean> canAct(@PathVariable Long id, @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(requisitionService.canAct(id, actor));
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
    public ResponseEntity<Void> processPayment(@PathVariable Long id) {

        this.requisitionService.processPayment(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload invoice", description = "Creates an invoice for a requisition. Requires a PDF file and invoice metadata. The vendor is auto-resolved from the selected quote.")
    @IsProcurementOfficer
    @PostMapping(value = "/{id}/invoice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InvoiceDto> createInvoice(
            @PathVariable Long id,
            @Valid @RequestPart("invoice") InvoiceCreateDto invoiceData,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        InvoiceDto invoice = requisitionService.createInvoice(id, invoiceData, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    @Operation(summary = "Get invoice", description = "Returns the invoice details for a requisition.")
    @IsRequester
    @GetMapping("/{id}/invoice")
    public ResponseEntity<InvoiceDto> getInvoice(@PathVariable Long id) {
        InvoiceDto invoice = requisitionService.getInvoice(id);
        return ResponseEntity.ok(invoice);
    }
}
