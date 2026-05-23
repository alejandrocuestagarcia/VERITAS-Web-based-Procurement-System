package com.veritas.backend.workflow.repository;

import com.veritas.backend.workflow.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

    @Query("SELECT w FROM WorkflowDefinition w WHERE " +
            "(:query IS NULL OR LOWER(w.name) LIKE :query OR LOWER(w.description) LIKE :query)" +
            " AND " +
            "(:isActive IS NULL OR w.isActive = :isActive)" +
            " AND " +
            "(:departmentId IS NULL OR w.department.departmentId = :departmentId OR " +
            "  (:includeGlobal = true AND w.department IS NULL))")
    Page<WorkflowDefinition> findAllFiltered(
            @Param("query") String query,
            @Param("isActive") Boolean isActive,
            @Param("departmentId") Long departmentId,
            @Param("includeGlobal") Boolean includeGlobal,
            Pageable pageable);

    List<WorkflowDefinition> findByIsActiveTrue();
}