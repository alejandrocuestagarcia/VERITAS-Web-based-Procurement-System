package com.veritas.backend.integrations.jira.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(nullable = false)
    private String apiToken;

    @Column(nullable = false, length = 1000)
    private String jql;

    @Column(nullable = false)
    private Integer syncIntervalMinutes;

    @Column(name = "custom_field_id", nullable = false)
    private String customFieldId;

    @Column(name = "last_sync_time")
    private java.time.LocalDateTime lastSyncTime;

    public java.time.LocalDateTime getNextSyncTime() {
        if (syncIntervalMinutes == null || syncIntervalMinutes <= 0) {
            return null;
        }
        if (lastSyncTime == null) {
            return java.time.LocalDateTime.now();
        }
        return lastSyncTime.plusMinutes(syncIntervalMinutes);
    }
}
