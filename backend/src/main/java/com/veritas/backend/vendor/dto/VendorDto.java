package com.veritas.backend.vendor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorDto (
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
    String primaryContactEmail
){
}
