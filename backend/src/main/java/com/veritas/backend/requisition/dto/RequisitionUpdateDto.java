package com.veritas.backend.requisition.dto;

import lombok.Data;

@Data
public class RequisitionUpdateDto {
    private String status;
    private String assignedTo;
}
