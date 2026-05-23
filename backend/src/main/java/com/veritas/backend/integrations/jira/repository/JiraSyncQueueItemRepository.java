package com.veritas.backend.integrations.jira.repository;

import com.veritas.backend.integrations.jira.entity.JiraSyncQueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JiraSyncQueueItemRepository extends JpaRepository<JiraSyncQueueItem, Long> {
    List<JiraSyncQueueItem> findByStatus(String status);
}
