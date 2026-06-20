package com.veritas.backend.department.service;

import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.department.dto.DepartmentCreateDto;
import com.veritas.backend.department.dto.DepartmentDto;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.department.service.DepartmentService;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DepartmentServiceIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @BeforeEach
    void setup() {
        projectRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    private Department createTestDepartment(String name) {
        InternalBudget budget = InternalBudget.builder()
                .budgetName(name)
                .budgetType(BudgetType.DEPARTMENT)
                .totalAmount(BigDecimal.valueOf(100000.0))
                .build();
        return Department.builder()
                .name(name)
                .internalBudget(budget)
                .build();
    }

    @Test
    void CreateDepartment_SavesToDatabase() {
        DepartmentCreateDto request = new DepartmentCreateDto("Engineering", BigDecimal.valueOf(10000.0));

        DepartmentDto result = departmentService.createDepartment(request);

        assertAll(
            () -> assertNotNull(result),
            () -> assertNotNull(result.id()),
            () -> assertEquals("Engineering", result.name())
        );

        List<Department> departments = departmentRepository.findAll();
        assertEquals(1, departments.size());
        assertEquals("Engineering", departments.get(0).getName());
    }

    @Test
    void CreateDepartment_DuplicateName_ThrowsEntityExistsException() {
        departmentRepository.save(createTestDepartment("Engineering"));
        DepartmentCreateDto request = new DepartmentCreateDto("Engineering", BigDecimal.valueOf(10000.0));

        assertThrows(EntityExistsException.class, () -> departmentService.createDepartment(request));
    }

    @Test
    void GetAllDepartments_RetrievesSavedDepartments() {
        departmentRepository.save(createTestDepartment("Engineering"));
        departmentRepository.save(createTestDepartment("Marketing"));

        List<DepartmentDto> result = departmentService.getAllDepartments();

        assertEquals(2, result.size());
        List<String> names = result.stream().map(DepartmentDto::name).toList();
        assertAll(
            () -> assertTrue(names.contains("Engineering")),
            () -> assertTrue(names.contains("Marketing"))
        );
    }

    @Test
    void GetDepartmentById_RetrievesCorrectDepartment() {
        Department saved = departmentRepository.save(createTestDepartment("Engineering"));

        DepartmentDto result = departmentService.getDepartmentById(saved.getDepartmentId());

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(saved.getDepartmentId(), result.id()),
            () -> assertEquals("Engineering", result.name())
        );
    }

    @Test
    void UpdateDepartment_PersistsChanges() {
        Department saved = departmentRepository.save(createTestDepartment("Engineering"));
        DepartmentCreateDto request = new DepartmentCreateDto("R&D", BigDecimal.valueOf(10000.0));

        DepartmentDto result = departmentService.updateDepartment(saved.getDepartmentId(), request);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals("R&D", result.name())
        );

        Department updated = departmentRepository.findById(saved.getDepartmentId()).orElseThrow();
        assertEquals("R&D", updated.getName());
    }

    @Test
    void UpdateDepartment_DuplicateName_ThrowsEntityExistsException() {
        Department dept1 = departmentRepository.save(createTestDepartment("Engineering"));
        departmentRepository.save(createTestDepartment("R&D"));

        DepartmentCreateDto request = new DepartmentCreateDto("R&D", BigDecimal.valueOf(10000.0));

        assertThrows(EntityExistsException.class,
                () -> departmentService.updateDepartment(dept1.getDepartmentId(), request));
    }

    @Test
    void DeleteDepartment_RemovesFromDatabase() {
        Department saved = departmentRepository.save(createTestDepartment("Engineering"));

        departmentService.deleteDepartment(saved.getDepartmentId());

        assertFalse(departmentRepository.existsById(saved.getDepartmentId()));
    }

    @Test
    void DeleteDepartment_NotFound_ThrowsEntityNotFoundException() {
        assertThrows(EntityNotFoundException.class, () -> departmentService.deleteDepartment(999L));
    }

    @Test
    void DeleteDepartment_WhenReferencedByTeam_ThrowsIllegalStateException() {
        Department saved = departmentRepository.save(createTestDepartment("Engineering"));

        teamRepository.save(Team.builder()
                .name("Team Alpha")
                .description("Main team")
                .department(saved)
                .isActive(true)
                .build());

        assertThrows(IllegalStateException.class,
                () -> departmentService.deleteDepartment(saved.getDepartmentId()));
    }
}
