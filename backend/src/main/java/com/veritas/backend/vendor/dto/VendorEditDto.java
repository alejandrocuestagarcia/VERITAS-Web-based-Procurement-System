package com.veritas.backend.vendor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record VendorEditDto(
        @Size(max = 120, message = "Vendor name must be at most 120 characters")
        String vendorName,

        @Size(max = 60, message = "Tax ID must be at most 60 characters")
        String taxId,

        @Size(max = 3000, message = "Description must be at most 3000 characters")
        String description,

        @Size(max = 120, message = "Primary contact name must be at most 120 characters")
        String primaryContactName,
        
        @Email(message = "Invalid email format")
        @Size(max = 120, message = "Primary contact email must be at most 120 characters")
        String primaryContactEmail
) {
}
