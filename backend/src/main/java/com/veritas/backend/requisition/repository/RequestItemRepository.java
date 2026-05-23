package com.veritas.backend.requisition.repository;

import com.veritas.backend.requisition.entity.RequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestItemRepository extends JpaRepository<RequestItem, Long> {

    @Modifying
    @Query("DELETE FROM RequestItem ri WHERE ri.request.requestID = :requestId")
    void deleteByRequestID(@Param("requestId") Long requestId);
}
