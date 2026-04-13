package com.veritas.backend.requisition.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class RequestQuoteId implements Serializable {
    private Long requestId;
    private Long quoteId;
}