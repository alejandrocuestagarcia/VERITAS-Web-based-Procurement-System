package com.veritas.backend.budget.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class BudgetDto {
    private Long id;
    @PositiveOrZero
    private Double totalAmount;
    @PositiveOrZero
    private Double safetyBuffer;
}
