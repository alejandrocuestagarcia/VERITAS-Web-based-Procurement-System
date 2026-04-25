package com.veritas.backend.vendor.repository;

import com.veritas.backend.vendor.dto.VendorScoreDto;
import com.veritas.backend.vendor.entity.VendorEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface VendorEvaluationRepository extends JpaRepository<VendorEvaluation, Long> {

    @Query("SELECT " +
            "e.vendor, " +
            "AVG(e.communicationScore), " +
            "AVG(e.deliveryScore), " +
            "AVG(e.qualityScore), " +
            "AVG((e.communicationScore + e.deliveryScore + e.qualityScore) / 3.0) " +
            "FROM VendorEvaluation e " +
            "GROUP BY e.vendor")
    List<VendorScoreDto> getAverageScores();
}
