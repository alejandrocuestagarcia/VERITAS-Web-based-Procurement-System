package com.veritas.backend.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "workflow_transitions")
@Data
public class WorkflowTransition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_step_id")
    private WorkflowStep fromStep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_step_id")
    private WorkflowStep toStep;

}