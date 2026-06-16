package com.veritas.backend.budget.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.veritas.backend.budget.dto.BudgetDto;
import com.veritas.backend.budget.entity.InternalBudget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

class BudgetMapperUnitTest {

    private BudgetMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(BudgetMapper.class);
    }

    @Test
    void toDto_NullBudget_ReturnsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void toDto_ValidBudget_MapsCorrectly() {
        InternalBudget budget = InternalBudget.builder()
                .id(1L)
                .totalAmount(new BigDecimal("1000.50"))
                .safetyBuffer(new BigDecimal("5.00"))
                .build();

        BudgetDto dto = mapper.toDto(budget);

        assertAll(
            () -> assertEquals(1L, dto.id()),
            () -> assertEquals(1000.50, dto.totalAmount()),
            () -> assertEquals(5.00, dto.safetyBuffer())
        );
    }

    @Test
    void toDto_NullAmounts_MapsNullFields() {
        InternalBudget budget = InternalBudget.builder()
                .id(1L)
                .totalAmount(null)
                .safetyBuffer(null)
                .build();

        BudgetDto dto = mapper.toDto(budget);

        assertAll(
            () -> assertEquals(1L, dto.id()),
            () -> assertNull(dto.totalAmount()),
            () -> assertNull(dto.safetyBuffer())
        );
    }
}
