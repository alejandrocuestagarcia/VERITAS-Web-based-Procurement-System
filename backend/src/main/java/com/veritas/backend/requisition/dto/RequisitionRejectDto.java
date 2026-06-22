package com.veritas.backend.requisition.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RequisitionRejectDto {
    @Size(max = 1000, message = "Reason must be at most 1000 characters")
    private String reason;
    private Boolean revisionRequired;
}
