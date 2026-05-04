package com.veritas.backend.requisition.repository;

import com.veritas.backend.requisition.entity.RequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestItemRepository extends JpaRepository<RequestItem, Long> {
}
