package com.veritas.backend.integrations.jira.entity;

import com.veritas.backend.project.entity.Project;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.veritas.backend.config.JpaEncryptionConverter;
import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "jira_configs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"jiraUrl", "jql"})
})
@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JiraConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String jiraUrl;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, length = 1024)
    @Convert(converter = JpaEncryptionConverter.class)
    private String apiToken;

    @Column(nullable = false, length = 1000)
    private String jql;

    @Column(nullable = false)
    private int syncIntervalMinutes;

    @Column(name = "custom_field_id", nullable = false)
    private String customFieldId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "fallback_user_id", nullable = false)
    private User fallbackUser;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "fallback_project_id", nullable = false)
    private Project fallbackProject;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "fallback_workflow_id", nullable = false)
    private WorkflowDefinition fallbackWorkflow;

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;

    public LocalDateTime getNextSyncTime() {
        if (syncIntervalMinutes <= 0) {
            return null;
        }
        if (lastSyncTime == null) {
            return LocalDateTime.now();
        }
        return lastSyncTime.plusMinutes(syncIntervalMinutes);
    }
}
