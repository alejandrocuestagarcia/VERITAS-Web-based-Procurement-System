package com.veritas.backend.department.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.department.dto.DepartmentCreateDto;
import com.veritas.backend.department.dto.DepartmentDto;
import com.veritas.backend.department.entity.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

class DepartmentMapperUnitTest {

    private DepartmentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(DepartmentMapper.class);
    }

    @Test
    void toDepartment_NullDto_ReturnsNull() {
        assertNull(mapper.toDepartment((DepartmentDto) null));
    }

    @Test
    void toDepartment_ValidDto_MapsCorrectly() {
        DepartmentDto dto = new DepartmentDto(
            5L,
            "Sales",
            BigDecimal.valueOf(5000),
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(500),
            BigDecimal.valueOf(100)
        );

        Department entity = mapper.toDepartment(dto);

        assertAll(
            () -> assertEquals(5L, entity.getDepartmentId()),
            () -> assertEquals("Sales", entity.getName())
        );
    }

    @Test
    void toDepartmentDto_NullEntity_ReturnsNull() {
        assertNull(mapper.toDepartmentDto(null));
    }

    @Test
    void toDepartmentDto_NullBudget_MapsCorrectly() {
        Department entity = new Department();
        entity.setDepartmentId(10L);
        entity.setName("Marketing");
        entity.setInternalBudget(null);

        DepartmentDto dto = mapper.toDepartmentDto(entity);

        assertAll(
            () -> assertEquals(10L, dto.id()),
            () -> assertEquals("Marketing", dto.name()),
            () -> assertNull(dto.budget()),
            () -> assertNull(dto.committedSpend()),
            () -> assertNull(dto.actualSpend()),
            () -> assertNull(dto.safetyBuffer())
        );
    }

    @Test
    void toDepartmentDto_WithBudget_MapsCorrectly() {
        InternalBudget budget = new InternalBudget();
        budget.setTotalAmount(BigDecimal.valueOf(10000));
        budget.setCommittedSpend(BigDecimal.valueOf(2000));
        budget.setActualSpend(BigDecimal.valueOf(1500));
        budget.setSafetyBuffer(BigDecimal.valueOf(500));

        Department entity = new Department();
        entity.setDepartmentId(10L);
        entity.setName("Marketing");
        entity.setInternalBudget(budget);

        DepartmentDto dto = mapper.toDepartmentDto(entity);

        assertAll(
            () -> assertEquals(10L, dto.id()),
            () -> assertEquals("Marketing", dto.name()),
            () -> assertEquals(BigDecimal.valueOf(10000), dto.budget()),
            () -> assertEquals(BigDecimal.valueOf(2000), dto.committedSpend()),
            () -> assertEquals(BigDecimal.valueOf(1500), dto.actualSpend()),
            () -> assertEquals(BigDecimal.valueOf(500), dto.safetyBuffer())
        );
    }

    @Test
    void toDepartmentFromCreateDto_NullDto_ReturnsNull() {
        assertNull(mapper.toDepartment((DepartmentCreateDto) null));
    }

    @Test
    void toDepartmentFromCreateDto_MapsCorrectly() {
        DepartmentCreateDto dto = new DepartmentCreateDto("HR", BigDecimal.valueOf(8000));
        Department entity = mapper.toDepartment(dto);

        assertAll(
            () -> assertEquals("HR", entity.getName()),
            () -> assertNull(entity.getDepartmentId())
        );
    }
}
