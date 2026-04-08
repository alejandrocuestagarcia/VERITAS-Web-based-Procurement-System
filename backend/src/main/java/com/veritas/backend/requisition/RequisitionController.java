package com.veritas.backend.requisition;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/requisitions")
@Tag(name = "Requisition Module", description = "Management of procurement requests")
public class RequisitionController {
    @Operation(summary = "Create a request", description = "Creates a new procurement request.")
    @PostMapping
    public String createRequest(@RequestBody Map<String, Object> requestBody) {
        return "Request created successfully";
    }

    @Operation(summary = "Get all requests (Search/Filter)", description = "List requests with filters for status and search terms.")
    @GetMapping
    public List<Object> getRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return List.of();
    }

    @Operation(summary = "Get pending actions", description = "Returns requests specifically awaiting action from the logged-in user.")
    @GetMapping("/pending")
    public List<Object> getPendingRequests() {
        return List.of();
    }

    @Operation(summary = "Get request details", description = "Returns all details for a single requisition.")
    @GetMapping("/{id}")
    public Object getRequestById(@PathVariable Long id) {
        return Map.of("id", id, "status", "DRAFT");
    }

    @Operation(summary = "Update/Edit request", description = "Edit draft details or change the assigned requester.")
    @PatchMapping("/{id}")
    public String updateRequest(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return "Request " + id + " updated";
    }

    @Operation(summary = "Submit request", description = "Finalizes a draft and moves it into the workflow engine.")
    @PostMapping("/{id}/submit")
    public String submitRequest(@PathVariable Long id) {
        return "Request " + id + " submitted to workflow";
    }

    @Operation(summary = "Approve request", description = "Moves the request to the next workflow step.")
    @PostMapping("/{id}/approve")
    public String approveRequest(@PathVariable Long id) {
        return "Request " + id + " approved";
    }

    @Operation(summary = "Reject request", description = "Rejects the request. Requires a reason in the body.")
    @PostMapping("/{id}/reject")
    public String rejectRequest(@PathVariable Long id, @RequestBody Map<String, String> rejectionData) {
        String reason = rejectionData.getOrDefault("reason", "No reason provided");
        return "Request " + id + " rejected. Reason: " + reason;
    }

    @Operation(summary = "Bulk upload quotes", description = "Uploads a CSV file containing multiple vendor quotes for a specific request.")
    @PostMapping(value = "/{id}/quotes/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadQuotes(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return "File " + file.getOriginalFilename() + " uploaded for request " + id;
    }

    @Operation(summary = "Get quote comparison", description = "Returns a side-by-side comparison of quotes, including external market price data.")
    @GetMapping("/{id}/comparison")
    public Object getQuoteComparison(@PathVariable Long id) {
        return Map.of(
                "requestId", id,
                "quotes", List.of(),
                "marketAverage", 0.0
        );
    }

    @Operation(summary = "Process final payment", description = "Finalizes a request, marks it as paid, and transitions funds from 'committed' to 'actual' in the budget.")
    @PostMapping("/{id}/pay")
    public String processPayment(@PathVariable Long id) {
        return "Invoice for request " + id + " marked as paid. Budget updated.";
    }
}
