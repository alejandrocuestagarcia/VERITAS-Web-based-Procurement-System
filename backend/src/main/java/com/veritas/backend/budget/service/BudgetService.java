package com.veritas.backend.budget.service;

import com.veritas.backend.budget.dto.BudgetDto;

public interface BudgetService {
    BudgetDto createBudget(BudgetDto budgetDto);
    BudgetDto editBudget(BudgetDto budgetDto);
}
