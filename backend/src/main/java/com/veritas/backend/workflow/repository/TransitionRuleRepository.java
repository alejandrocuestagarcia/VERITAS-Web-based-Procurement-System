package com.veritas.backend.workflow.repository;

import com.veritas.backend.workflow.entity.TransitionRule;
import com.veritas.backend.workflow.entity.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Repository
public interface TransitionRuleRepository extends JpaRepository<TransitionRule, Long> {
    Optional<TransitionRule> findByTransition(WorkflowTransition transition);
}