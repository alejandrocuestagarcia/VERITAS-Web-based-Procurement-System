package com.veritas.backend.requisition.repository;

import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    Optional<Request> findByJiraIssueKey(String jiraIssueKey);
    List<Request> findByJiraConfigId(Long jiraConfigId);

    @Query("SELECT r FROM Request r WHERE r.user.id = :userId AND r.state <> 'FINISHED' AND r.deletedAt IS NULL")
    List<Request> findActiveRequestsByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM Request r " +
            "LEFT JOIN r.project p " +
            "LEFT JOIN r.currentStep s " +
            "LEFT JOIN r.user u " +
            "LEFT JOIN r.assignee a " +
            "LEFT JOIN r.team t " +
            "LEFT JOIN t.department d " +
            "WHERE (r.deletedAt IS NULL OR r.closedReason = 'REJECTED' OR r.closedReason = 'CANCELLED') " +
            "AND (:status IS NULL OR " +
            "      (:status = 'OPEN' AND r.state <> 'FINISHED') OR " +
            "      (:status = 'CLOSED' AND r.state = 'FINISHED' AND r.closedReason = 'COMPLETED') OR " +
            "      (:status = 'REJECTED' AND r.state = 'FINISHED' AND r.closedReason = 'REJECTED') OR " +
            "      (:status = 'CANCELLED' AND r.state = 'FINISHED' AND r.closedReason = 'CANCELLED')) " +
            "AND (:userId IS NULL OR u.id = :userId) " +
            "AND (:assigneeId IS NULL OR a IS NULL OR a.id = :assigneeId) " +
            "AND (:teamId IS NULL OR t.teamId = :teamId) " +
            "AND (:departmentId IS NULL OR d.departmentId = :departmentId) " +
            "AND (:projectId IS NULL OR p.id = :projectId) " +
            "AND (CAST(:createdFrom AS LocalDateTime) IS NULL OR r.createdAt >= :createdFrom) " +
            "AND (CAST(:createdTo AS LocalDateTime) IS NULL OR r.createdAt <= :createdTo) " +
            "AND (:search IS NULL OR LOWER(r.requestName) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) " +
            "OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) " +
            "OR LOWER(r.requestKey) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))")
    Page<Request> findFilteredRequests(@Param("status") String status,
                                     @Param("search") String search,
                                     @Param("projectId") Long projectId,
                                     @Param("userId") Long userId,
                                     @Param("assigneeId") Long assigneeId,
                                     @Param("teamId") Long teamId,
                                     @Param("departmentId") Long departmentId,
                                     @Param("createdFrom") LocalDateTime createdFrom,
                                     @Param("createdTo") LocalDateTime createdTo,
                                     Pageable pageable);

    boolean existsByProjectId(Long projectId);

    boolean existsByTeamTeamId(Long teamId);

    boolean existsByTeamTeamIdAndStateNot(Long teamId, RequestStatus state);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Request r WHERE r.project.id = :projectId AND r.state <> 'FINISHED'")
    boolean existsActiveRequisitionsByProjectId(@Param("projectId") Long projectId);
}
