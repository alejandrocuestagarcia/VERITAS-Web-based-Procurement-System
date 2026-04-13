package com.veritas.backend.vendor.dto;

import lombok.Data;

@Data
public class VendorDto {
    private Long id;
    private String name;
    private Double reliabilityScore;
}
