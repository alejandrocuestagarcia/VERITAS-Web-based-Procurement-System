package com.veritas.backend.requisition.repository;

import com.veritas.backend.requisition.entity.Request;
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
}