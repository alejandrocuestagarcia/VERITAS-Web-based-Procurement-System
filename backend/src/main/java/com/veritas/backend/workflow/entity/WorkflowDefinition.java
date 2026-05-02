package com.veritas.backend.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_definitions")
@Data
public class WorkflowDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @OneToOne
    private WorkflowDefinition previousVersion;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false)
    private Integer version;
    @Column(nullable = false)
    private Boolean isActive;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String bpmnXml;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
    private LocalDateTime deactivatedAt;

    @PrePersist
    protected void onCreate() {
        this.isActive = true;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}