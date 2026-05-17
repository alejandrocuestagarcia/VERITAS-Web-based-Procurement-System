package com.veritas.backend.requisition.dto;

import java.math.BigDecimal;

public record QuoteLineItemDto(
    Long lineItemId,
    String productDescription,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal,
    Long requestItemId
) {}
