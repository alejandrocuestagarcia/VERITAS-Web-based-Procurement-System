package com.veritas.backend.budget.service;

import com.veritas.backend.budget.dto.BudgetDto;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

public interface BudgetService {

    /**
     * Creates a new global budget. Only one global budget can exist in the system.
     *
     * @param budgetDto the budget details to create
     * @return the created budget details as a DTO
     * @throws EntityExistsException if a global budget already exists
     */
    BudgetDto createBudget(BudgetDto budgetDto);

    /**
     * Edits the existing global budget.
     *
     * @param budgetDto the updated budget details
     * @return the updated budget details as a DTO
     * @throws EntityNotFoundException if no global budget exists
     */
    BudgetDto editBudget(BudgetDto budgetDto);
}
