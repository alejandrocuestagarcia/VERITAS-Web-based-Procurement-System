package com.veritas.backend.vendor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorDto (
    @NotBlank(message = "Vendor name is required")
    @Size(max = 120, message = "Team name must be at most 120 characters")
    String vendorName,
    @Size(max = 50, message = "Team name must be at most 120 characters")
    @NotBlank(message = "Tax ID is required")
    String taxId,
    Double communicationScore,
    Double deliveryScore,
    Double qualityScore,
    Double overallScore,
    @Size(max = 500, message = "Team name must be at most 120 characters")
    @NotBlank(message = "Description is required")
    String description,
    @Size(max = 50, message = "Team name must be at most 120 characters")
    String primaryContactName,
    @Size(max = 50, message = "Team name must be at most 120 characters")
    @Email(message = "Invalid email format")
    String primaryContactEmail
){
}
