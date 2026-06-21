package com.veritas.backend.budget.dto;

public record DepartmentDashboardBudgetDto(
    String department,
    Double budget,
    Double spent,
    Double committed,
    Double safetyBuffer
) {}
