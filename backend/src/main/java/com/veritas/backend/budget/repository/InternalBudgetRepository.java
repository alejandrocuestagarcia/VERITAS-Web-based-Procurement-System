package com.veritas.backend.budget.repository;

import com.veritas.backend.budget.entity.InternalBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veritas.backend.budget.entity.BudgetType;
import java.util.Optional;

@Repository
public interface InternalBudgetRepository extends JpaRepository<InternalBudget, Long> {
    Optional<InternalBudget> findByBudgetType(BudgetType budgetType);
    boolean existsByBudgetType(BudgetType budgetType);
}