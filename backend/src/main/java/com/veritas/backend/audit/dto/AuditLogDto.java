package com.veritas.backend.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.Instant;

@Data
@Schema(description = "Represents a single immutable entry in the audit trail")
public class AuditLogDto {

    @Schema(description = "Unique ID of the log entry", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "The ID of the requisition being audited", example = "101")
    private Long requestId;

    @Schema(description = "User who performed the action", example = "user_123")
    private String userId;

    @Schema(description = "Timestamp of the event", example = "2026-04-07T14:30:00Z")
    private Instant timestamp;

    @Schema(description = "The type of action performed", example = "STATUS_UPDATE")
    private String action;

    @Schema(description = "The state before the change", example = "DRAFT")
    private String previousStatus;

    @Schema(description = "The state after the change", example = "SUBMITTED")
    private String newStatus;

    @Schema(description = "SHA-256 hash of this entry for integrity", example = "a8f5f16...")
    private String currentHash;

    @Schema(description = "Hash of the preceding entry in the chain", example = "b7e4d15...")
    private String previousHash;
}