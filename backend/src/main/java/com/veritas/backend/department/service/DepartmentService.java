package com.veritas.backend.department.service;

import com.veritas.backend.department.dto.DepartmentCreateDto;
import com.veritas.backend.department.dto.DepartmentDto;

import java.util.List;

public interface DepartmentService {
    public DepartmentDto createDepartment(DepartmentCreateDto request);
    public List<DepartmentDto> getAllDepartments();
    public DepartmentDto getDepartmentById(Long id);
}
