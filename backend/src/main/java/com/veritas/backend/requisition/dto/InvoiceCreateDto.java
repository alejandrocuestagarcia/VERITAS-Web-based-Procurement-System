package com.veritas.backend.requisition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.veritas.backend.integrations.currency.entity.Currency;

@Data
public class InvoiceCreateDto {

    @NotBlank(message = "Invoice number is required")
    private String invoiceNumber;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;

    @NotNull(message = "Currency is required")
    private Currency currency;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private LocalDate invoiceDate;
}
