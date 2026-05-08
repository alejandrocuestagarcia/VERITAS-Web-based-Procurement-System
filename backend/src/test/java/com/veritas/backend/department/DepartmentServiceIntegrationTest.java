package com.veritas.backend.department;

import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.department.dto.DepartmentCreateDto;
import com.veritas.backend.department.dto.DepartmentDto;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.department.service.DepartmentService;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void CreateDepartment_SavesToDatabase() {
        DepartmentCreateDto request = new DepartmentCreateDto("Engineering");

        DepartmentDto result = departmentService.createDepartment(request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
        assertThat(result.name()).isEqualTo("Engineering");

        List<Department> departments = departmentRepository.findAll();
        assertThat(departments).hasSize(1);
        assertThat(departments.get(0).getName()).isEqualTo("Engineering");
    }

    @Test
    void CreateDepartment_DuplicateName_ThrowsEntityExistsException() {
        departmentRepository.save(Department.builder().name("Engineering").build());
        DepartmentCreateDto request = new DepartmentCreateDto("Engineering");

        assertThrows(EntityExistsException.class, () -> departmentService.createDepartment(request));
    }

    @Test
    void GetAllDepartments_RetrievesSavedDepartments() {
        departmentRepository.save(Department.builder().name("Engineering").build());
        departmentRepository.save(Department.builder().name("Marketing").build());

        List<DepartmentDto> result = departmentService.getAllDepartments();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(DepartmentDto::name).containsExactlyInAnyOrder("Engineering", "Marketing");
    }

    @Test
    void GetDepartmentById_RetrievesCorrectDepartment() {
        Department saved = departmentRepository.save(Department.builder().name("Engineering").build());

        DepartmentDto result = departmentService.getDepartmentById(saved.getDepartmentId());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(saved.getDepartmentId());
        assertThat(result.name()).isEqualTo("Engineering");
    }

    @Test
    void UpdateDepartment_PersistsChanges() {
        Department saved = departmentRepository.save(Department.builder().name("Engineering").build());
        DepartmentCreateDto request = new DepartmentCreateDto("R&D");

        DepartmentDto result = departmentService.updateDepartment(saved.getDepartmentId(), request);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("R&D");

        Department updated = departmentRepository.findById(saved.getDepartmentId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("R&D");
    }

    @Test
    void UpdateDepartment_DuplicateName_ThrowsEntityExistsException() {
        Department dept1 = departmentRepository.save(Department.builder().name("Engineering").build());
        departmentRepository.save(Department.builder().name("R&D").build());

        DepartmentCreateDto request = new DepartmentCreateDto("R&D");

        assertThrows(EntityExistsException.class, () -> departmentService.updateDepartment(dept1.getDepartmentId(), request));
    }

    @Test
    void DeleteDepartment_RemovesFromDatabase() {
        Department saved = departmentRepository.save(Department.builder().name("Engineering").build());

        departmentService.deleteDepartment(saved.getDepartmentId());

        assertThat(departmentRepository.existsById(saved.getDepartmentId())).isFalse();
    }

    @Test
    void DeleteDepartment_NotFound_ThrowsEntityNotFoundException() {
        assertThrows(EntityNotFoundException.class, () -> departmentService.deleteDepartment(999L));
    }
}
