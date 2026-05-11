package com.veritas.backend.budget.entity;

import com.veritas.backend.project.entity.Project;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "internal_budgets")
@Data
public class InternalBudget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budget_id")
    private Long id;

    @Column(name = "fiscal_year")
    private Integer fiscalYear;

    @Column(name = "fiscal_year_total")
    private BigDecimal fiscalYearTotal;

    @Column(name = "committed_spend")
    private BigDecimal committedSpend = BigDecimal.ZERO;

    @Column(name = "actual_spend")
    private BigDecimal actualSpend = BigDecimal.ZERO;

    @Column(name = "safety_buffer")
    private BigDecimal safetyBuffer = BigDecimal.ZERO;
}