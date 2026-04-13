package com.veritas.backend.workflow.repository;

import com.veritas.backend.workflow.entity.TransitionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TransitionRuleRepository extends JpaRepository<TransitionRule, Long> {

}