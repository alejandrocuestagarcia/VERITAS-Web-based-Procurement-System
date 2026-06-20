package com.veritas.backend.requisition.dto;

import java.time.LocalDateTime;

public record AttachmentDto(
        Long attachmentId,
        String fileName,
        String fileType,
        Long fileSize,
        String storagePath,
        LocalDateTime uploadedAt,
        Long invoiceId
) {}
