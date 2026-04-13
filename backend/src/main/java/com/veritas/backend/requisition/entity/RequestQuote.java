package com.veritas.backend.requisition.entity;

import com.veritas.backend.vendor.entity.Quote;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_quotes")
@Data
public class RequestQuote {
    @EmbeddedId
    private RequestQuoteId id = new RequestQuoteId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("requestId")
    @JoinColumn(name = "request_id")
    private Request request;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("quoteId")
    @JoinColumn(name = "quote_id")
    private Quote quote;

    private Boolean isSelected = false;
    private LocalDateTime createdAt = LocalDateTime.now();
}