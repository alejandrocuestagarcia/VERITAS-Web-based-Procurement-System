package com.veritas.backend.budget.dto;

import lombok.Data;
import java.util.List;

@Data
public class BudgetDashboardDto {
    private Double totalBudget;
    private Double committedFunds;
    private Double actualSpend;
    private Double safetyBuffer;
    private Boolean exists;
    private List<Double> burndownData;
    private List<Object> departmentData;
    private Double projectedBurn;
    private Double fiscalRunway;
}
