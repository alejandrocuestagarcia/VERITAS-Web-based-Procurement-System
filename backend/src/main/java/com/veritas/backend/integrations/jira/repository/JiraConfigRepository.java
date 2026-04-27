package com.veritas.backend.integrations.jira.repository;

import com.veritas.backend.integrations.jira.entity.JiraConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JiraConfigRepository extends JpaRepository<JiraConfig, Long> {
    boolean existsByJiraUrlAndJql(String jiraUrl, String jql);
    Optional<JiraConfig> findByJiraUrlAndJql(String jiraUrl, String jql);
}
