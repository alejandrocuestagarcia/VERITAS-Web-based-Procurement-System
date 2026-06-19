package com.veritas.backend.workflow.entity;

import com.veritas.backend.department.entity.Department;
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
    private int version;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String bpmnXml;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    private LocalDateTime deactivatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    public boolean getIsActive() {
        return this.isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    @PrePersist
    protected void onCreate() {
        this.isActive = true;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}