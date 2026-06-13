package com.veritas.backend.department.service;

import com.veritas.backend.department.dto.DepartmentCreateDto;
import com.veritas.backend.department.dto.DepartmentDto;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

import java.util.List;

public interface DepartmentService {

    /**
     * Creates a new department.
     *
     * @param request the department creation payload
     * @return the created department as a {@link DepartmentDto}
     * @throws EntityExistsException if a department with the same name already exists
     */
    public DepartmentDto createDepartment(DepartmentCreateDto request);

    /**
     * Returns all departments.
     *
     * @return a list of all departments as {@link DepartmentDto}
     */
    public List<DepartmentDto> getAllDepartments();

    /**
     * Returns a single department by its ID.
     *
     * @param id the department ID
     * @return the matching department as a {@link DepartmentDto}
     * @throws EntityNotFoundException if no department exists with the given ID
     */
    public DepartmentDto getDepartmentById(Long id);

    /**
     * Updates an existing department.
     *
     * @param id the ID of the department to update
     * @param request the updated department payload
     * @return the updated department as a {@link DepartmentDto}
     * @throws EntityNotFoundException if no department exists with the given ID
     * @throws EntityExistsException if another department already uses the requested name
     */
    public DepartmentDto updateDepartment(Long id, DepartmentCreateDto request);

    /**
     * Deletes a department by ID. Deletion is blocked if any teams or users are still associated with it.
     *
     * @param id the ID of the department to delete
     * @throws ntityNotFoundException if no department exists with the given ID
     * @throws IllegalStateException if the department still has associated teams or users
     */
    public void deleteDepartment(Long id);
}
