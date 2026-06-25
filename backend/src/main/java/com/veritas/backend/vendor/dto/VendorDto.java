package com.veritas.backend.vendor.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorDto (
    Long id,
    @NotBlank(message = "Vendor name is required")
    @Size(max = 120, message = "Vendor name must be at most 120 characters")
    String vendorName,
    @NotBlank(message = "Tax ID is required")
    @Size(max = 60, message = "Tax ID must be at most 60 characters")
    String taxId,
    Double communicationScore,
    Double deliveryScore,
    Double qualityScore,
    Double gapScore,
    Double overallScore,
    Long evaluationCount,
    @NotBlank(message = "Description is required")
    @Size(max = 3000, message = "Description must be at most 3000 characters")
    String description,
    @Size(max = 120, message = "Primary contact name must be at most 120 characters")
    String primaryContactName,
    @Email(message = "Invalid email format")
    @Size(max = 120, message = "Primary contact email must be at most 120 characters")
    String primaryContactEmail,
    LocalDateTime updatedAt,
    LocalDateTime createdAt,
    LocalDateTime deletedAt
){
}
