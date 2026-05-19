package com.veritas.backend.department.mapper;

import com.veritas.backend.department.dto.DepartmentCreateDto;
import com.veritas.backend.department.dto.DepartmentDto;
import com.veritas.backend.department.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface DepartmentMapper {
    @Mapping(source = "id", target = "departmentId")
    Department toDepartment(DepartmentDto departmentDto);

    @Mapping(source = "departmentId", target = "id")
    @Mapping(source = "internalBudget.totalAmount", target = "budget")
    @Mapping(source = "internalBudget.committedSpend", target = "committedSpend")
    @Mapping(source = "internalBudget.actualSpend", target = "actualSpend")
    @Mapping(source = "internalBudget.safetyBuffer", target = "safetyBuffer")
    DepartmentDto toDepartmentDto(Department department);

    Department toDepartment(DepartmentCreateDto departmentCreateDto);
}
