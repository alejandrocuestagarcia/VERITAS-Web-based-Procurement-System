package com.veritas.backend.vendor.dto;

import java.time.LocalDateTime;

public record VendorDto (
    String vendorName,
    String taxId,
    Double communicationScore,
    Double deliveryScore,
    Double qualityScore,
    Double overallScore,
    String description,
    String primaryContactName,
    String primaryContactEmail,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
){}
