package com.veritas.backend.requisition.dto;

import com.veritas.backend.requisition.entity.RequestItemUnit;

public record RequisitionItemDto(
    Long id,
    String name,
    Integer quantity,
    RequestItemUnit unit,
    String description
) {}
