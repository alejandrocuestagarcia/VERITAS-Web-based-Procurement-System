package com.veritas.backend.budget.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "internal_budgets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalBudget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budget_id")
    private Long id;

    @Column(name = "budget_name")
    private String budgetName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_budget_id")
    private InternalBudget parentBudget;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(name = "committed_spend", nullable = false)
    private BigDecimal committedSpend = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "actual_spend", nullable = false)
    private BigDecimal actualSpend = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "safety_buffer", nullable = false)
    private BigDecimal safetyBuffer = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_type")
    private BudgetType budgetType;
}