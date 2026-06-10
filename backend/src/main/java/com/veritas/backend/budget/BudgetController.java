package com.veritas.backend.budget;

import com.veritas.backend.budget.dto.BudgetDashboardDto;
import com.veritas.backend.budget.dto.BudgetDto;
import com.veritas.backend.config.annotations.IsFinanceOfficer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.veritas.backend.budget.service.BudgetService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/budget")
@RequiredArgsConstructor
@Tag(name = "Financial Governance Module", description = "Budgeting, project tracking, and final expenditure processing")
public class BudgetController {

    private final BudgetService budgetService;

    @Operation(summary = "Finance Dashboard", description = "Aggregated statistics including burndown charts and committed spending vs. actual spend.")
    @IsFinanceOfficer
    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public BudgetDashboardDto getFinanceDashboard(@RequestParam(required = false) Long departmentId) {
        return budgetService.getFinanceDashboard(departmentId);
    }

    @Operation(summary = "Create Budget", description = "Create company budget, fails if a budget already exists.")
    @IsFinanceOfficer
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public BudgetDto createBudget(@RequestBody BudgetDto budgetDto) {
        return budgetService.createBudget(budgetDto);
    }

    @Operation(summary = "Edit Budget", description = "Edit the existing company budget")
    @IsFinanceOfficer
    @PatchMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public BudgetDto editBudget(@RequestBody BudgetDto budgetDto) {
        return budgetService.editBudget(budgetDto);
    }
}