package com.veritas.backend.department.service.impl;

import com.veritas.backend.department.dto.DepartmentCreateDto;
import com.veritas.backend.department.dto.DepartmentDto;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.mapper.DepartmentMapper;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.department.service.DepartmentService;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentDto createDepartment(DepartmentCreateDto request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new EntityExistsException("Department with name " + request.name() + " already exists");
        }

        Department department = departmentMapper.toDepartment(request);
        return departmentMapper.toDepartmentDto(departmentRepository.save(department));
    }

    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toDepartmentDto)
                .toList();
    }

    public DepartmentDto getDepartmentById(Long id) {
        return departmentMapper.toDepartmentDto(departmentRepository.getDepartmentByDepartmentId(id));
    }

    @Override
    public DepartmentDto updateDepartment(Long id, DepartmentCreateDto request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Department not found with id " + id));
        if (departmentRepository.existsByName(request.name()) && !department.getName().equals(request.name())) {
            throw new EntityExistsException("Department with name " + request.name() + " already exists");
        }
        department.setName(request.name());
        return departmentMapper.toDepartmentDto(departmentRepository.save(department));
    }

    @Override
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new jakarta.persistence.EntityNotFoundException("Department not found with id " + id);
        }
        departmentRepository.deleteById(id);
    }
}
