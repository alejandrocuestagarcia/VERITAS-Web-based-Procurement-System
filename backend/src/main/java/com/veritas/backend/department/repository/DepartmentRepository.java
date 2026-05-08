package com.veritas.backend.department.repository;

import com.veritas.backend.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByName(String name);

    Department getDepartmentByDepartmentId(Long departmentId);
}
