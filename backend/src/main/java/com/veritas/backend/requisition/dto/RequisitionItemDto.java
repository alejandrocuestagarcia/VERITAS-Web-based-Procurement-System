package com.veritas.backend.requisition.dto;

public record RequisitionItemDto(
    Long id,
    String name,
    Integer quantity,
    String unit,
    String description
) {}
