package com.veritas.backend.workflow.repository;

import com.veritas.backend.workflow.entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {

}