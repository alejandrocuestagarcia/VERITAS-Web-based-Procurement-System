package com.veritas.backend.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.Instant;

@Data
@Schema(description = "Represents a single immutable entry in the audit trail")
public class AuditLogDto {

    private String id;
    private Long requestId;
    private String userId;
    private Instant timestamp;
    private String action;
    private String previousStatus;
    private String newStatus;
    private String currentHash;
    private String previousHash;
}