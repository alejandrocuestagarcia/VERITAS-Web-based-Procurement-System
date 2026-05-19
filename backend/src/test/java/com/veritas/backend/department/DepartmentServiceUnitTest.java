package com.veritas.backend.department;

import com.veritas.backend.department.dto.DepartmentCreateDto;
import com.veritas.backend.department.dto.DepartmentDto;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.mapper.DepartmentMapper;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.department.service.impl.DepartmentServiceImpl;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import com.veritas.backend.team.repository.TeamRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceUnitTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;
    
    @Mock
    private TeamRepository teamRepository;

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

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Engineering");

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
    void GetAllDepartments_ReturnsAll() {
        Department dept1 = Department.builder().departmentId(1L).name("Engineering").build();
        Department dept2 = Department.builder().departmentId(2L).name("HR").build();

        DepartmentDto dto1 = new DepartmentDto(1L, "Engineering", BigDecimal.valueOf(10000.0), null, null, null);
        DepartmentDto dto2 = new DepartmentDto(2L, "HR", BigDecimal.valueOf(10000.0), null, null, null);

        when(departmentRepository.findAll()).thenReturn(List.of(dept1, dept2));
        when(departmentMapper.toDepartmentDto(dept1)).thenReturn(dto1);
        when(departmentMapper.toDepartmentDto(dept2)).thenReturn(dto2);

        List<DepartmentDto> result = departmentService.getAllDepartments();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Engineering");
        assertThat(result.get(1).name()).isEqualTo("HR");

        verify(departmentRepository).findAll();
    }

    @Test
    void GetDepartmentById_ValidId_ReturnsDepartment() {
        Department department = Department.builder().departmentId(1L).name("Engineering").build();
        DepartmentDto dto = new DepartmentDto(1L, "Engineering", BigDecimal.valueOf(10000.0), null, null, null);

        when(departmentRepository.getDepartmentByDepartmentId(1L)).thenReturn(department);
        when(departmentMapper.toDepartmentDto(department)).thenReturn(dto);

        DepartmentDto result = departmentService.getDepartmentById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Engineering");

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

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("R&D");

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

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Engineering");

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
    void DeleteDepartment_ValidId_Deletes() {
        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(teamRepository.existsByDepartmentDepartmentId(1L)).thenReturn(false);

        departmentService.deleteDepartment(1L);

        verify(departmentRepository).existsById(1L);
        verify(teamRepository).existsByDepartmentDepartmentId(1L);
        verify(departmentRepository).deleteById(1L);
    }

    @Test
    void DeleteDepartment_ReferencedByTeams_ThrowsDataIntegrityViolationException() {
        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(teamRepository.existsByDepartmentDepartmentId(1L)).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class, () -> departmentService.deleteDepartment(1L));

        verify(departmentRepository, never()).deleteById(any());
    }

    @Test
    void DeleteDepartment_NotFound_ThrowsEntityNotFoundException() {
        when(departmentRepository.existsById(1L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> departmentService.deleteDepartment(1L));

        verify(departmentRepository, never()).deleteById(any());
    }
}
