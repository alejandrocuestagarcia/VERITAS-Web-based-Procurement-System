package com.veritas.backend.requisition.repository;

import com.veritas.backend.vendor.entity.Quote;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@NoRepositoryBean
public interface RequestQuoteRepository extends Repository<Quote, Long> {
}
