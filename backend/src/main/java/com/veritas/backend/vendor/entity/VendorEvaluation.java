package com.veritas.backend.vendor.entity;

import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_evaluations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"vendor_id", "request_id"}))
@Data
public class VendorEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long evaluationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private User evaluator;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", unique = true, nullable = false)
    private Request request;

    @Column(name = "delivery_score", nullable = false)
    private int deliveryScore;

    @Column(name = "quality_score", nullable = false)
    private int qualityScore;

    @Column(name = "communication_score", nullable = false)
    private int communicationScore;

    @Column(name = "gap_score", nullable = false)
    private Double gapScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}