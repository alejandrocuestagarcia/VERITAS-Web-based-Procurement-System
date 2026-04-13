package com.veritas.backend.workflow.repository;

import com.veritas.backend.workflow.entity.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {

}