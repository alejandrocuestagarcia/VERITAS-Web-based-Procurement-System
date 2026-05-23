package com.veritas.backend.vendor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record VendorRatingDto(
    @NotNull @Min(0) @Max(10) Integer communicationScore,
    @NotNull @Min(0) @Max(10) Integer deliveryScore,
    @NotNull @Min(0) @Max(10) Integer qualityScore,
    String notes
) {}
