package com.veritas.backend.vendor.repository;

import com.veritas.backend.vendor.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {
    java.util.List<Quote> findByRequestRequestID(Long requestID);

    @Modifying
    @Query("DELETE FROM Quote q WHERE q.request.requestID = :requestId")
    void deleteByRequestID(@Param("requestId") Long requestId);
}