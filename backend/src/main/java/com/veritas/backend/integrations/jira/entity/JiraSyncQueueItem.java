package com.veritas.backend.integrations.jira.entity;

import com.veritas.backend.requisition.entity.Request;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "jira_sync_queue_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraSyncQueueItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jira_config_id", nullable = false)
    private JiraConfig jiraConfig;

    @Column(name = "jira_issue_key", nullable = false)
    private String jiraIssueKey;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    @Builder.Default
    private Integer retries = 0;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_attempt")
    private LocalDateTime lastAttempt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
