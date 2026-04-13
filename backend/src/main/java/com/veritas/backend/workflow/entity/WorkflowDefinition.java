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

    private String name;
    private String description;
    private Integer version = 1;
    private Boolean isActive = true;
    private String layoutJson;
    private LocalDateTime createdDate = LocalDateTime.now();
    private LocalDateTime updatedDate;
    private LocalDateTime deactivatedDate;
}