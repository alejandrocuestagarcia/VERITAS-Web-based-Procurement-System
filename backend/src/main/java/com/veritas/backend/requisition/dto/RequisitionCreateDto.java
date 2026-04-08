package com.veritas.backend.requisition.dto;

import lombok.Data;

@Data
public class RequisitionCreateDto {
    private String title;
    private String description;
    private Double amount;
}
