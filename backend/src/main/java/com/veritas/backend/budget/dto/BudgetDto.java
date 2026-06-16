package com.veritas.backend.budget.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record BudgetDto(
    Long id,
    @PositiveOrZero Double totalAmount,
    @PositiveOrZero Double safetyBuffer
) {}
