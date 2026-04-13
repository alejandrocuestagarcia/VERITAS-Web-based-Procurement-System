package com.veritas.backend.vendor.dto;

import lombok.Data;

@Data
public class VendorRatingDto {
    private Double communicationScore;
    private Double deliveryScore;
    private Double qualityScore;
}
