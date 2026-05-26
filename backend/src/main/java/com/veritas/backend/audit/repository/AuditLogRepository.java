package com.veritas.backend.audit.repository;

import com.veritas.backend.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.workflow.entity.WorkflowStep;

import java.util.Optional;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("""
    SELECT a FROM AuditLog a
    LEFT JOIN FETCH a.actor
    LEFT JOIN FETCH a.request
    LEFT JOIN FETCH a.previousStep
    LEFT JOIN FETCH a.newStep
    LEFT JOIN FETCH a.transition
    WHERE a.action = :action
    AND (:search IS NULL OR
        LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')) OR
        (a.actor IS NOT NULL AND LOWER(a.actor.email) LIKE LOWER(CONCAT('%', :search, '%'))) OR
        (a.actor IS NULL AND LOWER('system') LIKE LOWER(CONCAT('%', :search, '%'))))
    """)
    Page<AuditLog> findAllByAction(@Param("action") String action, Pageable pageable, @Param("search") String search);

    Optional<AuditLog> findFirstByRequestAndNewStepAndActionOrderByTimestampDesc(
            Request request,
            WorkflowStep newStep,
            String action
    );

    Optional<AuditLog> findFirstByRequestAndNewStepOrderByTimestampAsc(
            Request request,
            WorkflowStep newStep
    );

    Optional<AuditLog> findFirstByRequestAndPreviousStepAndActionOrderByTimestampDesc(
            Request request,
            WorkflowStep previousStep,
            String action
    );
}