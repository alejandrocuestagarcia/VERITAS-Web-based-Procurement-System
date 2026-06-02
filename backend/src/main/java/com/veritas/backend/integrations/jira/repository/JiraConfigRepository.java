package com.veritas.backend.integrations.jira.repository;

import com.veritas.backend.integrations.jira.entity.JiraConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface JiraConfigRepository extends JpaRepository<JiraConfig, Long> {
    boolean existsByJiraUrlAndJql(String jiraUrl, String jql);
    Optional<JiraConfig> findByJiraUrlAndJql(String jiraUrl, String jql);

    @Query("SELECT j FROM JiraConfig j WHERE " +
            "(:search IS NULL OR (" +
            "LOWER(j.name) LIKE :search OR " +
            "LOWER(j.jiraUrl) LIKE :search OR " +
            "LOWER(j.jql) LIKE :search))")
    Page<JiraConfig> findAllFiltered(@Param("search") String search, Pageable pageable);

    boolean existsByFallbackProjectId(Long projectId);
}
