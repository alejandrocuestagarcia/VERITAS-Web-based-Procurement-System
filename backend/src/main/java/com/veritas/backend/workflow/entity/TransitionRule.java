package com.veritas.backend.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "transition_rules")
@Data
public class TransitionRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "transition_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private WorkflowTransition transition;

    private Integer minRequiredVendors = 0;
    private Boolean isPdfRequired = false;
    private Boolean isCsvRequired = false;
    private Boolean isImageRequired = false;
}