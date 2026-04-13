package com.veritas.backend.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "transition_rules")
@Data
public class TransitionRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "transition_id")
    private WorkflowTransition transition;

    private Integer minRequiredVendors = 0;
    private Boolean isPdfRequired = false;
    private String optionalFailureMessage;
}