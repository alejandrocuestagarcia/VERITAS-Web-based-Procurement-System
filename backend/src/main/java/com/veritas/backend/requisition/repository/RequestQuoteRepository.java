package com.veritas.backend.requisition.repository;

import com.veritas.backend.requisition.entity.RequestQuote;
import com.veritas.backend.requisition.entity.RequestQuoteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface RequestQuoteRepository extends JpaRepository<RequestQuote, RequestQuoteId> {

}