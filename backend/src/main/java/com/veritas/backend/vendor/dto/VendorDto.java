package com.veritas.backend.vendor.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VendorDto (
    Long id,
    @NotBlank(message = "Vendor name is required")
    String vendorName,
    @NotBlank(message = "Tax ID is required")
    String taxId,
    Double communicationScore,
    Double deliveryScore,
    Double qualityScore,
    Double overallScore,
    @NotBlank(message = "Description is required")
    String description,
    String primaryContactName,
    @Email(message = "Invalid email format")
    String primaryContactEmail,
    LocalDateTime updatedAt,
    LocalDateTime createdAt,
    LocalDateTime deletedAt
){
}
