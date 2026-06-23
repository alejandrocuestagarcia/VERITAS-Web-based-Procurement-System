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

    @Column(name = "required_file_types")
    private String requiredFileTypes;

    @Column(name = "min_vendor_reliability_score")
    private Double minVendorReliabilityScore;

    @Column(name = "advanced_rule")
    private String advancedRule;
}