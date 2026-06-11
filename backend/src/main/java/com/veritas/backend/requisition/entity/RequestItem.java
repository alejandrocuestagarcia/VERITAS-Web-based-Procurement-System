package com.veritas.backend.requisition.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "request_items")
@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RequestItemUnit unit;

    @Column(columnDefinition = "TEXT")
    private String description;
}
