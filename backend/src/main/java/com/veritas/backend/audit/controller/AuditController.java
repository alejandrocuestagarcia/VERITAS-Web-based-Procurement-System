package com.veritas.backend.audit.controller;

import com.veritas.backend.audit.dto.AuditLogDto;
import com.veritas.backend.audit.service.AuditService;
import com.veritas.backend.config.annotations.IsRequester;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/requisitions/{requestId}/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Module", description = "Traceability and history for procurement requests")
public class AuditController {

    private final AuditService auditService;

    @Operation(summary = "Get audit logs", description = "Retrieves a chronological list of all actions and state changes for a request.")
    @IsRequester
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AuditLogDto> getAuditLogs(@PathVariable Long requestId) {
        return auditService.getAuditLogsByRequestId(requestId);
    }

    @Operation(summary = "Export audit as PDF", description = "Generates and downloads a PDF report of the request lifecycle.")
    @IsRequester
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> exportAuditPdf(@PathVariable Long requestId) {
        byte[] pdfContent = new byte[0];
        ByteArrayResource resource = new ByteArrayResource(pdfContent);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit_report_req_" + requestId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @Operation(summary = "Get audit trace graph", description = "Returns audit entries linked with hash pointers for integrity visualization.")
    @IsRequester
    @GetMapping("/trace")
    public List<AuditLogDto> getAuditTrace(@PathVariable Long requestId) {
        return List.of();
    }
}