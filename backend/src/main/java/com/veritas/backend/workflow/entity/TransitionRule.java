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

    @Column(name = "min_required_vendors", nullable = false)
    private int minRequiredVendors = 0;

    @Column(name = "required_file_types")
    private String requiredFileTypes;

    @Column(name = "min_vendor_reliability_score")
    private Double minVendorReliabilityScore;

    @Column(name = "advanced_rule")
    private String advancedRule;

    public boolean getIsPdfRequired() {
        return this.isPdfRequired;
    }

    public boolean getIsCsvRequired() {
        return this.isCsvRequired;
    }

    public boolean getIsImageRequired() {
        return this.isImageRequired;
    }

    public void setIsPdfRequired(boolean isPdfRequired) {
        this.isPdfRequired = isPdfRequired;
    }

    public void setIsCsvRequired(boolean isCsvRequired) {
        this.isCsvRequired = isCsvRequired;
    }

    public void setIsImageRequired(boolean isImageRequired) {
        this.isImageRequired = isImageRequired;
    }
}