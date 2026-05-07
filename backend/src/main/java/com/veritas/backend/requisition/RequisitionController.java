package com.veritas.backend.requisition;

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
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(path = "/requisitions", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Requisition Module", description = "Management of procurement requests")
public class RequisitionController {
    private final RequisitionService requisitionService;

    @Operation(summary = "Create a request", description = "Creates a new procurement request.")
    @IsRequester
    @PostMapping
    public ResponseEntity<RequisitionDto> createRequest(@Valid @RequestBody RequisitionCreateDto requestBody,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requisitionService.createRequest(requestBody, user));
    }

    @Operation(summary = "Get all requests (Search/Filter)", description = "List requests with filters for status and search terms.")
    @IsFinanceOfficer
    @GetMapping
    public ResponseEntity<List<RequisitionDto>> getRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(List.of());
    }

    @Operation(summary = "Get pending actions", description = "Returns requests specifically awaiting action from the logged-in user.")
    @IsRequester
    @GetMapping("/pending")
    public ResponseEntity<List<RequisitionDto>> getPendingRequests() {
        return ResponseEntity.ok(List.of());
    }

    @Operation(summary = "Get request details", description = "Returns all details for a single requisition.")
    @IsRequester
    @GetMapping("/{id}")
    public ResponseEntity<RequisitionDto> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(null);
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
    public ResponseEntity<RequisitionDto> submitRequest(@PathVariable Long id) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Approve request", description = "Moves the request to the next workflow step.")
    @IsProcurementOfficer
    @PostMapping("/{id}/approve")
    public ResponseEntity<RequisitionDto> approveRequest(@PathVariable Long id) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Reject request", description = "Rejects the request. Requires a reason in the body.")
    @IsProcurementOfficer
    @PostMapping("/{id}/reject")
    public ResponseEntity<RequisitionDto> rejectRequest(@PathVariable Long id, @RequestBody RequisitionRejectDto rejectionData) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "Bulk upload quotes", description = "Uploads a CSV file containing multiple vendor quotes for a specific request.")
    @IsRequester
    @PostMapping(value = "/{id}/quotes/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadQuotes(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        requisitionService.saveAttachment(id, file);
        return ResponseEntity.ok("File " + file.getOriginalFilename() + " uploaded for request " + id);
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
