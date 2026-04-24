package com.veritas.backend.vendor.dto;

public record VendorScoreDto(String taxId, Double communicationScore, Double deliveryScore, Double qualityScore,
                             Double overallScore) {

}
