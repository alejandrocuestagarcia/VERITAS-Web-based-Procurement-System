package com.veritas.backend.requisition.repository;

import com.veritas.backend.requisition.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    Optional<Request> findByJiraIssueKey(String jiraIssueKey);
}