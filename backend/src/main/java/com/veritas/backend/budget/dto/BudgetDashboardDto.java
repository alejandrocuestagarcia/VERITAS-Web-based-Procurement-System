package com.veritas.backend.budget.dto;

import java.util.List;

public record BudgetDashboardDto(
    Double totalBudget,
    Double committedFunds,
    Double actualSpend,
    Double safetyBuffer,
    Boolean exists,
    List<Double> burndownData,
    List<DepartmentDashboardBudgetDto> departmentData,
    Double projectedBurn,
    Double fiscalRunway
) {}
