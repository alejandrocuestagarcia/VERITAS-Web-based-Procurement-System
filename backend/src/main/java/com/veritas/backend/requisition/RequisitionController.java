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
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/requisitions")
@RequiredArgsConstructor
@Tag(name = "Requisition Module", description = "Management of procurement requests")
public class RequisitionController {
    private final RequisitionService requisitionService;

    @Operation(summary = "Create a request", description = "Creates a new procurement request.")
    @IsRequester
    @PostMapping
    public RequisitionDto createRequest(@RequestBody RequisitionCreateDto requestBody,
            @AuthenticationPrincipal User user) {
        return requisitionService.createRequest(requestBody, user);
    }

    @Operation(summary = "Get all requests (Search/Filter)", description = "List requests with filters for status and search terms.")
    @IsFinanceOfficer
    @GetMapping
    public List<RequisitionDto> getRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return List.of();
    }

    @Operation(summary = "Get pending actions", description = "Returns requests specifically awaiting action from the logged-in user.")
    @IsRequester
    @GetMapping("/pending")
    public List<RequisitionDto> getPendingRequests() {
        return List.of();
    }

    @Operation(summary = "Get request details", description = "Returns all details for a single requisition.")
    @IsRequester
    @GetMapping("/{id}")
    public RequisitionDto getRequestById(@PathVariable Long id) {
        RequisitionDto dto = new RequisitionDto();
        dto.setId(id);
        dto.setStatus("DRAFT");
        return dto;
    }

    @Operation(summary = "Update/Edit request", description = "Edit draft details or change the assigned requester.")
    @IsRequester
    @PatchMapping("/{id}")
    public RequisitionDto updateRequest(@PathVariable Long id, @RequestBody RequisitionUpdateDto updates) {
        return new RequisitionDto();
    }

    @Operation(summary = "Submit request", description = "Finalizes a draft and moves it into the workflow engine.")
    @IsRequester
    @PostMapping("/{id}/submit")
    public RequisitionDto submitRequest(@PathVariable Long id) {
        return new RequisitionDto();
    }

    @Operation(summary = "Approve request", description = "Moves the request to the next workflow step.")
    @IsProcurementOfficer
    @PostMapping("/{id}/approve")
    public RequisitionDto approveRequest(@PathVariable Long id) {
        return new RequisitionDto();
    }

    @Operation(summary = "Reject request", description = "Rejects the request. Requires a reason in the body.")
    @IsProcurementOfficer
    @PostMapping("/{id}/reject")
    public RequisitionDto rejectRequest(@PathVariable Long id, @RequestBody RequisitionRejectDto rejectionData) {
        return new RequisitionDto();
    }

    @Operation(summary = "Bulk upload quotes", description = "Uploads a CSV file containing multiple vendor quotes for a specific request.")
    @IsRequester
    @PostMapping(value = "/{id}/quotes/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadQuotes(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return "File " + file.getOriginalFilename() + " uploaded for request " + id;
    }

    @Operation(summary = "Get quote comparison", description = "Returns a side-by-side comparison of quotes, including external market price data.")
    @IsRequester
    @GetMapping("/{id}/comparison")
    public QuoteComparisonDto getQuoteComparison(@PathVariable Long id) {
        QuoteComparisonDto dto = new QuoteComparisonDto();
        dto.setRequestId(id);
        dto.setQuotes(List.of());
        dto.setMarketAverage(0.0);
        return dto;
    }

    @Operation(summary = "Process final payment", description = "Finalizes a request, marks it as paid, and transitions funds from 'committed' to 'actual' in the budget.")
    @IsFinanceOfficer
    @PostMapping("/{id}/pay")
    public RequisitionDto processPayment(@PathVariable Long id) {
        return new RequisitionDto();
    }
}
