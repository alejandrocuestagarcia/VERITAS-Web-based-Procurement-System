package com.veritas.backend.department.service.impl;

import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.department.dto.DepartmentCreateDto;
import com.veritas.backend.department.dto.DepartmentDto;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.mapper.DepartmentMapper;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.department.service.DepartmentService;
import jakarta.persistence.EntityNotFoundException;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.project.entity.Project;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.veritas.backend.budget.entity.InternalBudget;
import java.math.BigDecimal;
import java.util.List;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final InternalBudgetRepository internalBudgetRepository;
    private final ProjectRepository projectRepository;

    private void validateDepartmentBudgetLimit(Long excludeDepartmentId, BigDecimal departmentBudget) {
        InternalBudget parentBudget = internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL).orElse(null);
        if (parentBudget != null && parentBudget.getTotalAmount() != null) {
            BigDecimal existingTotal = departmentRepository.findAll().stream()
                    .filter(d -> excludeDepartmentId == null || !d.getDepartmentId().equals(excludeDepartmentId))
                    .map(d -> d.getInternalBudget() != null ? d.getInternalBudget().getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal limit = parentBudget.getTotalAmount();
            BigDecimal newTotal = existingTotal.add(departmentBudget);
            if (newTotal.compareTo(limit) > 0) {
                DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.GERMANY));
                throw new IllegalArgumentException("Department budget of " + df.format(departmentBudget) + "€"
                        + " exceeds the remaining global budget of " + df.format(limit.subtract(existingTotal)) + "€"
                        + " (Total Limit: " + df.format(limit) + "€)");
            }
        }
    }

    public DepartmentDto createDepartment(DepartmentCreateDto request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new EntityExistsException("Department with name " + request.name() + " already exists");
        }

        Department department = departmentMapper.toDepartment(request);
        InternalBudget parentBudget =  internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL).orElse(null);
        if (request.budget() != null) {
            validateDepartmentBudgetLimit(null, request.budget());
            InternalBudget budget = InternalBudget.builder().budgetName(department.getName()).budgetType(BudgetType.DEPARTMENT).totalAmount(request.budget()).parentBudget(parentBudget).build();
            department.setInternalBudget(budget);
        }
        
        return departmentMapper.toDepartmentDto(departmentRepository.save(department));
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toDepartmentDto)
                .toList();
    }

    @Transactional(readOnly = true)
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
        if (request.budget() != null) {
            InternalBudget parentBudget =  internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL).orElse(null);
            
            // Check global budget limit
            validateDepartmentBudgetLimit(id, request.budget());

            // Check if projects budgets exceed the new department budget
            List<Project> projects = projectRepository.findByTeamDepartment(department);
            BigDecimal projectsBudgetSum = projects.stream()
                    .map(p -> p.getInternalBudget().getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (request.budget().compareTo(projectsBudgetSum) < 0) {
                DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.GERMANY));
                throw new IllegalArgumentException("New department budget of " + df.format(request.budget()) + "€"
                        + " is less than the sum of its projects' budgets (" + df.format(projectsBudgetSum) + "€)");
            }

            if (department.getInternalBudget() != null) {
                InternalBudget existingBudget = department.getInternalBudget();
                existingBudget.setBudgetName(department.getName());
                existingBudget.setTotalAmount(request.budget());
            } else {
                InternalBudget budget = InternalBudget.builder()
                        .budgetName(department.getName())
                        .budgetType(BudgetType.DEPARTMENT)
                        .totalAmount(request.budget())
                        .parentBudget(parentBudget)
                        .build();
                department.setInternalBudget(budget);
            }
        }
        return departmentMapper.toDepartmentDto(departmentRepository.save(department));
    }

    @Override
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new EntityNotFoundException("Department not found with id " + id);
        }
        
        if (teamRepository.existsByDepartmentDepartmentId(id)) {
            throw new IllegalStateException("Cannot delete department because there are teams pointing to it");
        }

        if (userRepository.existsByDepartmentDepartmentId(id)) {
            throw new IllegalStateException("Cannot delete department because there are users pointing to it");
        }

        departmentRepository.deleteById(id);
    }
}
