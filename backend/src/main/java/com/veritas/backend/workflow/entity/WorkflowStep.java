package com.veritas.backend.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "workflow_steps")
@Data
public class WorkflowStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id")
    private WorkflowDefinition workflowDefinition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowComponent workflowComponent;

    private String name;

    private String description;
}