package com.veritas.backend.vendor.repository;

import com.veritas.backend.vendor.entity.QuoteLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteLineItemRepository extends JpaRepository<QuoteLineItem, Long> {
    List<QuoteLineItem> findByQuoteQuoteID(Long quoteId);

    @Modifying
    @Query("DELETE FROM QuoteLineItem qli WHERE qli.quote.request.requestID = :requestId")
    void deleteByQuoteRequestID(@Param("requestId") Long requestId);
}