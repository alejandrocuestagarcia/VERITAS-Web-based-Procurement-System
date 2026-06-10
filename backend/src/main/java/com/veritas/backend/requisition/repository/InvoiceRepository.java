package com.veritas.backend.requisition.repository;

import com.veritas.backend.requisition.entity.Invoice;
import com.veritas.backend.requisition.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT MONTH(r.createdAt) as month, SUM(i.paidAmountEur) as total " +
            "FROM Invoice i " +
            "JOIN i.request r " +
            "WHERE i.isPaid = true AND YEAR(r.createdAt) = :year " +
            "GROUP BY MONTH(r.createdAt) " +
            "ORDER BY MONTH(r.createdAt) ASC")
    List<Object[]> findActualMonthlySpend(@Param("year") int year);

    @Query("SELECT MONTH(r.createdAt) as month, SUM(i.paidAmountEur) as total " +
            "FROM Invoice i " +
            "JOIN i.request r " +
            "JOIN r.project p " +
            "JOIN p.team t " +
            "WHERE i.isPaid = true AND YEAR(r.createdAt) = :year AND t.department.departmentId = :departmentId " +
            "GROUP BY MONTH(r.createdAt) " +
            "ORDER BY MONTH(r.createdAt) ASC")
    List<Object[]> findActualMonthlySpendByDepartment(@Param("year") int year, @Param("departmentId") Long departmentId);

    Optional<Invoice> findByRequest(Request request);
}