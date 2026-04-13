package com.veritas.backend.vendor.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Quote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_id")
    private Long quoteID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendorID;

    @Column(length = 3)
    private String currency;

    @Column(name = "base_amount")
    private BigDecimal baseAmount;

    @Column(name = "shipping_costs")
    private BigDecimal shippingCosts;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "converted_amount")
    private BigDecimal convertedAmount;

    @Column(name = "conversion_rate", precision = 19, scale = 10)
    private BigDecimal conversionRate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}