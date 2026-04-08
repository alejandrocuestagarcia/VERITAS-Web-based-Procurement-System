package com.veritas.backend.budget;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/budget")
@Tag(name = "Financial Governance Module", description = "Budgeting, project tracking, and final expenditure processing")
public class BudgetController {

    @Operation(summary = "Finance Dashboard", description = "Aggregated statistics including burndown charts and committed spending vs. actual spend.")
    @GetMapping("/dashboard")
    public Object getFinanceDashboard() {
        return Map.of(
                "totalBudget", 0.0,
                "committedFunds", 0.0,
                "actualSpend", 0.0,
                "burndownData", List.of()
        );
    }

    @Operation(summary = "Create Budget", description = "Create company budget, fails if a budget already exists.")
    @PostMapping
    public String createBudget() {
        return "Budget created";
    }

    @Operation(summary = "Edit Budget", description = "Edit the existing company budget")
    @PatchMapping
    public String editBudget() {
        return "Budget edited";
    }
}