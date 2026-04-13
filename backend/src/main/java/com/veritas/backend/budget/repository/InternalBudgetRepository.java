package com.veritas.backend.budget.repository;

import com.veritas.backend.budget.entity.InternalBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InternalBudgetRepository extends JpaRepository<InternalBudget, Long> {

}