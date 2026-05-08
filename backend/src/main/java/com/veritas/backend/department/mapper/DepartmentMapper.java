package com.veritas.backend.department.mapper;

import com.veritas.backend.department.dto.DepartmentCreateDto;
import com.veritas.backend.department.dto.DepartmentDto;
import com.veritas.backend.department.entity.Department;
import org.mapstruct.Mapper;

@Mapper(unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface DepartmentMapper {
    Department toDepartment(DepartmentDto departmentDto);
    DepartmentDto toDepartmentDto(Department department);
    Department toDepartment(DepartmentCreateDto departmentCreateDto);
}
