package com.veritas.backend.budget.dto;

import lombok.Data;
import java.util.List;

@Data
public class BudgetDashboardDto {
    private Double totalBudget;
    private Double committedFunds;
    private Double actualSpend;
    private List<Object> burndownData;
}
