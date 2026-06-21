package com.veritas.backend.budget.service;

import com.veritas.backend.budget.dto.BudgetDashboardDto;
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

    /**
     * Retrieves the financial dashboard metrics.
     * Computes the global budget, committed funds, actual spend, safety buffer,
     * monthly burndown data, departmental resource utilization, projected burn rate,
     * and fiscal runway. If a department ID is specified, the calculations are
     * filtered/scaled for that department.
     *
     * @param departmentId the optional department ID to filter the dashboard data
     * @return the financial dashboard metrics as a {@link BudgetDashboardDto}
     */
    BudgetDashboardDto getFinanceDashboard(Long departmentId);
}
