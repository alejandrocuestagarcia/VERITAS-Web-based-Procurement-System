package com.veritas.backend.requisition.repository;

import com.veritas.backend.requisition.entity.Invoice;
import com.veritas.backend.requisition.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByRequest(Request request);
}