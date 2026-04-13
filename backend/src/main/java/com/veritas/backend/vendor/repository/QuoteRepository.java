package com.veritas.backend.vendor.repository;

import com.veritas.backend.vendor.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {

}