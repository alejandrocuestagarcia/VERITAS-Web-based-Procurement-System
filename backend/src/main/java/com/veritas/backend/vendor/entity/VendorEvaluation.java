package com.veritas.backend.vendor.entity;

import com.veritas.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_evaluations")
@Data
public class VendorEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long evaluationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id")
    private User evaluator;

    private Integer deliveryScore;
    private Integer qualityScore;
    private Integer communicationScore;
    private String notes;
    private LocalDateTime createdAt = LocalDateTime.now();
}