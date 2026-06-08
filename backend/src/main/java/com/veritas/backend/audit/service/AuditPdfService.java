package com.veritas.backend.audit.service;

public interface AuditPdfService {
    byte[] generateAuditReport(Long requestId);
}
