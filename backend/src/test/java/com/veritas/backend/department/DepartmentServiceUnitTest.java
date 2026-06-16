package com.veritas.backend.department;

import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.department.dto.DepartmentCreateDto;
import com.veritas.backend.department.dto.DepartmentDto;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.mapper.DepartmentMapper;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.department.service.impl.DepartmentServiceImpl;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.user.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import com.veritas.backend.team.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceUnitTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private TeamRepository teamRepository;

    @Mock
    private InternalBudgetRepository internalBudgetRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    @Test
    void CreateDepartment_ValidRequest_SavesAndReturnsDepartment() {
        DepartmentCreateDto request = new DepartmentCreateDto("Engineering", BigDecimal.valueOf(10000.0));
        Department department = Department.builder().name("Engineering").build();
        Department savedDepartment = Department.builder().departmentId(1L).name("Engineering").build();
        DepartmentDto expectedDto = new DepartmentDto(1L, "Engineering", BigDecimal.valueOf(10000.0), null, null, null);

        when(departmentRepository.existsByName("Engineering")).thenReturn(false);
        when(departmentMapper.toDepartment(request)).thenReturn(department);
        when(departmentRepository.save(department)).thenReturn(savedDepartment);
        when(departmentMapper.toDepartmentDto(savedDepartment)).thenReturn(expectedDto);

        DepartmentDto result = departmentService.createDepartment(request);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(1L, result.id()),
            () -> assertEquals("Engineering", result.name())
        );

        verify(departmentRepository).existsByName("Engineering");
        verify(departmentRepository).save(department);
    }

    @Test
    void CreateDepartment_DuplicateName_ThrowsEntityExistsException() {
        DepartmentCreateDto request = new DepartmentCreateDto("Engineering", BigDecimal.valueOf(10000.0));

        when(departmentRepository.existsByName("Engineering")).thenReturn(true);

        assertThrows(EntityExistsException.class, () -> departmentService.createDepartment(request));

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void CreateDepartment_ExceedsGlobalBudget_ThrowsIllegalArgumentException() {
        DepartmentCreateDto request = new DepartmentCreateDto("Engineering", BigDecimal.valueOf(10000.0));
        Department existingDept = Department.builder()
                .departmentId(2L)
                .name("HR")
                .internalBudget(InternalBudget.builder().totalAmount(BigDecimal.valueOf(95000.0)).build())
                .build();
        InternalBudget globalBudget = InternalBudget.builder()
                .budgetType(BudgetType.GLOBAL)
                .totalAmount(BigDecimal.valueOf(100000.0))
                .build();

        when(departmentRepository.existsByName("Engineering")).thenReturn(false);
        when(internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)).thenReturn(Optional.of(globalBudget));
        when(departmentRepository.findAll()).thenReturn(List.of(existingDept));

        assertThrows(IllegalArgumentException.class, () -> departmentService.createDepartment(request));
    }

    @Test
    void GetAllDepartments_ReturnsAll() {
        Department dept1 = Department.builder().departmentId(1L).name("Engineering").build();
        Department dept2 = Department.builder().departmentId(2L).name("HR").build();

        DepartmentDto dto1 = new DepartmentDto(1L, "Engineering", BigDecimal.valueOf(10000.0), null, null, null);
        DepartmentDto dto2 = new DepartmentDto(2L, "HR", BigDecimal.valueOf(10000.0), null, null, null);

        when(departmentRepository.findAll()).thenReturn(List.of(dept1, dept2));
        when(departmentMapper.toDepartmentDto(dept1)).thenReturn(dto1);
        when(departmentMapper.toDepartmentDto(dept2)).thenReturn(dto2);

        List<DepartmentDto> result = departmentService.getAllDepartments();

        assertAll(
            () -> assertEquals(2, result.size()),
            () -> assertEquals("Engineering", result.get(0).name()),
            () -> assertEquals("HR", result.get(1).name())
        );

        verify(departmentRepository).findAll();
    }

    @Test
    void GetDepartmentById_ValidId_ReturnsDepartment() {
        Department department = Department.builder().departmentId(1L).name("Engineering").build();
        DepartmentDto dto = new DepartmentDto(1L, "Engineering", BigDecimal.valueOf(10000.0), null, null, null);

        when(departmentRepository.getDepartmentByDepartmentId(1L)).thenReturn(department);
        when(departmentMapper.toDepartmentDto(department)).thenReturn(dto);

        DepartmentDto result = departmentService.getDepartmentById(1L);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(1L, result.id()),
            () -> assertEquals("Engineering", result.name())
        );

        verify(departmentRepository).getDepartmentByDepartmentId(1L);
    }

    @Test
    void UpdateDepartment_ValidRequest_UpdatesAndReturns() {
        DepartmentCreateDto request = new DepartmentCreateDto("R&D", BigDecimal.valueOf(10000.0));
        Department existing = Department.builder().departmentId(1L).name("Engineering").build();
        Department saved = Department.builder().departmentId(1L).name("R&D").build();
        DepartmentDto expectedDto = new DepartmentDto(1L, "R&D", BigDecimal.valueOf(10000.0), null, null, null);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByName("R&D")).thenReturn(false);
        when(departmentRepository.save(existing)).thenReturn(saved);
        when(departmentMapper.toDepartmentDto(saved)).thenReturn(expectedDto);

        DepartmentDto result = departmentService.updateDepartment(1L, request);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(1L, result.id()),
            () -> assertEquals("R&D", result.name())
        );

        verify(departmentRepository).findById(1L);
        verify(departmentRepository).existsByName("R&D");
        verify(departmentRepository).save(existing);
    }

    @Test
    void UpdateDepartment_SameName_UpdatesAndReturnsWithoutCheckingDuplicate() {
        DepartmentCreateDto request = new DepartmentCreateDto("Engineering", BigDecimal.valueOf(10000.0));
        Department existing = Department.builder().departmentId(1L).name("Engineering").build();
        Department saved = Department.builder().departmentId(1L).name("Engineering").build();
        DepartmentDto expectedDto = new DepartmentDto(1L, "Engineering", BigDecimal.valueOf(10000.0), null, null, null);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        // existsByName is called in impl, but since name is same as existing, it shouldn't trigger duplicate exception
        when(departmentRepository.existsByName("Engineering")).thenReturn(true);
        when(departmentRepository.save(existing)).thenReturn(saved);
        when(departmentMapper.toDepartmentDto(saved)).thenReturn(expectedDto);

        DepartmentDto result = departmentService.updateDepartment(1L, request);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals("Engineering", result.name())
        );

        verify(departmentRepository).findById(1L);
        verify(departmentRepository).save(existing);
    }

    @Test
    void UpdateDepartment_NotFound_ThrowsEntityNotFoundException() {
        DepartmentCreateDto request = new DepartmentCreateDto("R&D", BigDecimal.valueOf(10000.0));

        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> departmentService.updateDepartment(1L, request));

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void UpdateDepartment_DuplicateName_ThrowsEntityExistsException() {
        DepartmentCreateDto request = new DepartmentCreateDto("R&D", BigDecimal.valueOf(10000.0));
        Department existing = Department.builder().departmentId(1L).name("Engineering").build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByName("R&D")).thenReturn(true);

        assertThrows(EntityExistsException.class, () -> departmentService.updateDepartment(1L, request));

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void UpdateDepartment_ExceedsGlobalBudget_ThrowsIllegalArgumentException() {
        DepartmentCreateDto request = new DepartmentCreateDto("Engineering", BigDecimal.valueOf(20000.0));
        Department currentDept = Department.builder()
                .departmentId(1L)
                .name("Engineering")
                .internalBudget(InternalBudget.builder().totalAmount(BigDecimal.valueOf(10000.0)).build())
                .build();
        Department otherDept = Department.builder()
                .departmentId(2L)
                .name("HR")
                .internalBudget(InternalBudget.builder().totalAmount(BigDecimal.valueOf(90000.0)).build())
                .build();
        InternalBudget globalBudget = InternalBudget.builder()
                .budgetType(BudgetType.GLOBAL)
                .totalAmount(BigDecimal.valueOf(100000.0))
                .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(currentDept));
        when(departmentRepository.existsByName("Engineering")).thenReturn(false);
        when(internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)).thenReturn(Optional.of(globalBudget));
        when(departmentRepository.findAll()).thenReturn(List.of(currentDept, otherDept));

        assertThrows(IllegalArgumentException.class, () -> departmentService.updateDepartment(1L, request));
    }

    @Test
    void UpdateDepartment_LowerThanProjectsBudgetSum_ThrowsIllegalArgumentException() {
        DepartmentCreateDto request = new DepartmentCreateDto("Engineering", BigDecimal.valueOf(5000.0));
        Department currentDept = Department.builder()
                .departmentId(1L)
                .name("Engineering")
                .internalBudget(InternalBudget.builder().totalAmount(BigDecimal.valueOf(10000.0)).build())
                .build();
        Project project1 = Project.builder()
                .id(101L)
                .internalBudget(InternalBudget.builder().totalAmount(BigDecimal.valueOf(4000.0)).build())
                .build();
        Project project2 = Project.builder()
                .id(102L)
                .internalBudget(InternalBudget.builder().totalAmount(BigDecimal.valueOf(3000.0)).build())
                .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(currentDept));
        when(departmentRepository.existsByName("Engineering")).thenReturn(false);
        when(projectRepository.findByTeamDepartment(currentDept)).thenReturn(List.of(project1, project2));

        assertThrows(IllegalArgumentException.class, () -> departmentService.updateDepartment(1L, request));
    }

    @Test
    void DeleteDepartment_ValidId_Deletes() {
        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(teamRepository.existsByDepartmentDepartmentId(1L)).thenReturn(false);
        when(userRepository.existsByDepartmentDepartmentId(1L)).thenReturn(false);

        departmentService.deleteDepartment(1L);

        verify(departmentRepository).existsById(1L);
        verify(teamRepository).existsByDepartmentDepartmentId(1L);
        verify(departmentRepository).deleteById(1L);
    }

    @Test
    void DeleteDepartment_ReferencedByTeams_ThrowsDataIntegrityViolationException() {
        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(teamRepository.existsByDepartmentDepartmentId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> departmentService.deleteDepartment(1L));

        verify(departmentRepository, never()).deleteById(any());
    }

    @Test
    void DeleteDepartment_NotFound_ThrowsEntityNotFoundException() {
        when(departmentRepository.existsById(1L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> departmentService.deleteDepartment(1L));

        verify(departmentRepository, never()).deleteById(any());
    }
}
