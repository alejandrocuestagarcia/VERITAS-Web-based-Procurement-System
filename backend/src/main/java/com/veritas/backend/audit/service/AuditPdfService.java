package com.veritas.backend.audit.service;

public interface AuditPdfService {

    /**
     * Generates a PDF audit report for the given request, including its metadata,
     * line items, and full audit log ordered by timestamp descending.
     *
     * @param requestId the ID of the request to generate the report for
     * @return a byte array containing the raw PDF file content
     * @throws EntityNotFoundException if no request exists with the given ID
     * @throws RuntimeException if PDF generation fails due to an I/O error
     */
    byte[] generateAuditReport(Long requestId);
}
