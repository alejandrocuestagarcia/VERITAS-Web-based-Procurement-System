package com.veritas.backend.vendor.repository;

import com.veritas.backend.vendor.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByTaxId(String taxId);

    @Query("SELECT v FROM Vendor v WHERE ((:search IS NULL OR LOWER(v.vendorName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "OR (:search IS NULL OR LOWER(v.taxId) LIKE LOWER(CONCAT('%', :search, '%')))) " +
            "AND COALESCE(v.overallScore, 0.0) >= :rating")
    Page<Vendor> findByRating(@Param("search") String search, @Param("rating") Double rating, Pageable pageable);

    @Query("SELECT AVG(v.overallScore) FROM Vendor v")
    Double getAverageOverallScore();
}