package com.veritas.backend.vendor.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Formula;

import java.time.LocalDateTime;

@Entity
@Table(name = "vendors")
@Data
public class Vendor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vendor_id")
    private Long id;

    @Column(name = "vendor_name", nullable = false)
    private String vendorName;

    @Column(name = "tax_id", unique = true)
    private String taxId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "primary_contact_name")
    private String primaryContactName;

    @Column(name = "primary_contact_email")
    private String primaryContactEmail;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Formula("(SELECT AVG(e.communication_score) FROM vendor_evaluations e WHERE e.vendor_id = vendor_id)")
    private Double communicationScore;

    @Formula("(SELECT AVG(e.quality_score) FROM vendor_evaluations e WHERE e.vendor_id = vendor_id)")
    private Double qualityScore;

    @Formula("(SELECT AVG(e.delivery_score) FROM vendor_evaluations e WHERE e.vendor_id = vendor_id)")
    private Double deliveryScore;

    @Formula("(SELECT AVG((COALESCE(e.communication_score, 0) + COALESCE(e.delivery_score, 0) + COALESCE(e.quality_score, 0)) / 3.0) FROM vendor_evaluations e WHERE e.vendor_id = vendor_id)")
    private Double overallScore;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}