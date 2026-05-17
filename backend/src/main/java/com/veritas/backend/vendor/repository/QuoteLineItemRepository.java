package com.veritas.backend.vendor.repository;

import com.veritas.backend.vendor.entity.QuoteLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteLineItemRepository extends JpaRepository<QuoteLineItem, Long> {
    List<QuoteLineItem> findByQuoteQuoteID(Long quoteId);
}