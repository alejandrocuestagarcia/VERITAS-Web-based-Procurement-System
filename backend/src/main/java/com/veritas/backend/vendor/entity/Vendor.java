package com.veritas.backend.vendor.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "Vendor name is required")
    @Size(max = 120, message = "Vendor name must be at most 120 characters")
    private String vendorName;

    @Column(name = "tax_id", unique = true)
    @NotBlank(message = "Tax ID is required")
    @Size(max = 60, message = "Tax ID must be at most 60 characters")
    private String taxId;

    @Column(columnDefinition = "TEXT")
    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @Column(name = "primary_contact_name")
    @NotBlank(message = "Primary contact name is required")
    @Size(max = 120, message = "Primary contact name must be at most 120 characters")
    private String primaryContactName;

    @Column(name = "primary_contact_email")
    @NotBlank(message = "Primary contact email is required")
    @Size(max = 120, message = "Primary contact email must be at most 120 characters")
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