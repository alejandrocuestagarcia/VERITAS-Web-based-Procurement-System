package com.veritas.backend.vendor.repository;

import com.veritas.backend.vendor.entity.VendorEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorEvaluationRepository extends JpaRepository<VendorEvaluation, Long> {

    boolean existsByVendorIdAndRequestRequestID(Long vendorId, Long requestId);
}
