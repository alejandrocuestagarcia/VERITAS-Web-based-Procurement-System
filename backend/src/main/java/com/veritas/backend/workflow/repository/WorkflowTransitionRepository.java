package com.veritas.backend.workflow.repository;

import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.entity.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {
    List<WorkflowTransition> findByFromStep(WorkflowStep fromStep);
}