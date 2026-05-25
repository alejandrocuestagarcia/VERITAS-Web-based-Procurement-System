package com.veritas.backend.workflow.repository;

import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
    Optional<WorkflowStep> findFirstByWorkflowDefinitionAndWorkflowComponent(WorkflowDefinition workflowDefinition, WorkflowComponent workflowComponent);
}