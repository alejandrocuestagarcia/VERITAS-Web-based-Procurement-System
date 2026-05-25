package com.veritas.backend.requisition.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record InvoiceDto(
    Long invoiceId,
    Long requestId,
    String invoiceNumber,
    LocalDate invoiceDate,
    BigDecimal totalAmount,
    LocalDate dueDate,
    Boolean isPaid,
    Long vendorId,
    String vendorName,
    LocalDateTime createdAt
) {}
