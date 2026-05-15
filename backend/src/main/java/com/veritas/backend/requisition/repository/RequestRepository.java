package com.veritas.backend.requisition.repository;

import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    Optional<Request> findByJiraIssueKey(String jiraIssueKey);

    @Query("SELECT r FROM Request r WHERE r.userID.id = :userId AND r.currentStepID.workflowComponent != 'END_EVENT'")
    List<Request> findActiveRequestsByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM Request r " +
            "LEFT JOIN r.projectID p " +
            "LEFT JOIN r.currentStepID s " +
            "LEFT JOIN r.userID u " +
            "LEFT JOIN r.teamID t " +
            "LEFT JOIN t.department d " +
            "WHERE (:status IS NULL OR " +
            "      (:status = 'OPEN' AND (s IS NULL OR s.workflowComponent <> :endEvent)) OR " +
            "      (:status = 'CLOSED' AND s IS NOT NULL AND s.workflowComponent = :endEvent)) " +
            "AND (:userId IS NULL OR u.id = :userId) " +
            "AND (:teamId IS NULL OR t.teamId = :teamId) " +
            "AND (:departmentId IS NULL OR d.departmentId = :departmentId) " +
            "AND (:projectId IS NULL OR p.id = :projectId) " +
            "AND (:search IS NULL OR LOWER(r.requestName) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) " +
            "OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) " +
            "OR LOWER(r.requestKey) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))")
    Page<Request> findFilteredRequests(@Param("status") String status,
                                     @Param("search") String search,
                                     @Param("projectId") Long projectId,
                                     @Param("userId") Long userId,
                                     @Param("teamId") Long teamId,
                                     @Param("departmentId") Long departmentId,
                                     @Param("endEvent") WorkflowComponent endEvent,
                                     Pageable pageable);
}