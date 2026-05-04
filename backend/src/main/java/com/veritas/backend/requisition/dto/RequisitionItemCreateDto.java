package com.veritas.backend.requisition.dto;

import java.math.BigDecimal;

public record RequisitionItemCreateDto(
    String name,
    Integer quantity,
    BigDecimal estimatedPrice,
    String description
) {}
