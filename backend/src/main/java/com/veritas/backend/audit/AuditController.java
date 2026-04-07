package com.veritas.backend.audit;

import com.veritas.backend.audit.dto.AuditLogDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/requests/{requestId}/audit")
@Tag(name = "Audit Module", description = "Traceability and history for procurement requests")
public class AuditController {

    /**
     * US-35: Audit Log View
     * Returns a list of all changes for a specific request.
     */
    @Operation(summary = "Get audit logs", description = "Retrieves a chronological list of all actions and state changes for a request.")
    @GetMapping
    public List<AuditLogDto> getAuditLogs(@PathVariable Long requestId) {
        // Implementation would call auditService.getLogsByRequestId(requestId)
        return List.of();
    }

    /**
     * US-36: Audit Export as PDF
     * Generates a PDF report of the audit trail.
     */
    @Operation(summary = "Export audit as PDF", description = "Generates and downloads a PDF report of the request lifecycle.")
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> exportAuditPdf(@PathVariable Long requestId) {
        // Mocking PDF generation
        byte[] pdfContent = new byte[0];
        ByteArrayResource resource = new ByteArrayResource(pdfContent);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit_report_req_" + requestId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    /**
     * US-56: Audit Trace Visualization
     * Returns data structured for a Git-like graph (including hashes).
     */
    @Operation(summary = "Get audit trace graph", description = "Returns audit entries linked with hash pointers for integrity visualization.")
    @GetMapping("/trace")
    public List<AuditLogDto> getAuditTrace(@PathVariable Long requestId) {
        // Logic to return nodes with previousHash and currentHash
        return List.of();
    }
}