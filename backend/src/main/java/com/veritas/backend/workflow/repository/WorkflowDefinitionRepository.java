package com.veritas.backend.workflow.repository;

import com.veritas.backend.workflow.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {
    List<WorkflowDefinition> findAllByIsActiveTrue();
}