package com.veritas.backend.vendor.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "quote_line_items")
@Data
public class QuoteLineItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lineItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id")
    private Quote quote;

    private String productDescription;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}