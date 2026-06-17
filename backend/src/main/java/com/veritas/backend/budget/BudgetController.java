package com.veritas.backend.budget;

import com.veritas.backend.budget.dto.BudgetDashboardDto;
import com.veritas.backend.budget.dto.BudgetDto;
import com.veritas.backend.config.annotations.IsFinanceOfficer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.veritas.backend.budget.service.BudgetService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/budget")
@RequiredArgsConstructor
@Tag(name = "Financial Governance Module", description = "Budgeting, project tracking, and final expenditure processing")
public class BudgetController {

    private final BudgetService budgetService;

    @Operation(summary = "Finance Dashboard", description = "Aggregated statistics including burndown charts and committed spending vs. actual spend.")
    @IsFinanceOfficer
    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BudgetDashboardDto> getFinanceDashboard(@RequestParam(required = false) Long departmentId) {
        return ResponseEntity.ok(budgetService.getFinanceDashboard(departmentId));
    }

    @Operation(summary = "Create Budget", description = "Create company budget, fails if a budget already exists.")
    @IsFinanceOfficer
    @PostMapping
    public ResponseEntity<BudgetDto> createBudget(@RequestBody @Valid BudgetDto budgetDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.createBudget(budgetDto));
    }

    @Operation(summary = "Edit Budget", description = "Edit the existing company budget")
    @IsFinanceOfficer
    @PatchMapping
    public ResponseEntity<BudgetDto> editBudget(@RequestBody @Valid BudgetDto budgetDto) {
        return ResponseEntity.ok(budgetService.editBudget(budgetDto));
    }
}