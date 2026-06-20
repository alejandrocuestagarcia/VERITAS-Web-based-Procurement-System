package com.veritas.backend.budget;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.veritas.backend.budget.dto.BudgetDto;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.mapper.BudgetMapper;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.budget.service.impl.BudgetServiceImpl;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetServiceUnitTest {

    @Mock
    private InternalBudgetRepository internalBudgetRepository;

    @Mock
    private BudgetMapper budgetMapper;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private BudgetDto fullDto;
    private BudgetDto nullFieldsDto;
    private InternalBudget globalBudget;

    @BeforeEach
    void setUp() {
        fullDto = new BudgetDto(1L, 1000.0, 50.0);
        nullFieldsDto = new BudgetDto(1L, null, null);
        globalBudget = new InternalBudget();
        globalBudget.setId(1L);
        globalBudget.setBudgetName("Global Budget");
        globalBudget.setBudgetType(BudgetType.GLOBAL);
        globalBudget.setTotalAmount(BigDecimal.valueOf(1000.0));
        globalBudget.setSafetyBuffer(BigDecimal.valueOf(50.0));
    }

    @Test
    void createBudget_GlobalBudgetAlreadyExists_ThrowsEntityExistsException() {
        when(internalBudgetRepository.existsByBudgetType(BudgetType.GLOBAL)).thenReturn(true);

        assertThrows(EntityExistsException.class, () -> budgetService.createBudget(fullDto));
        verify(internalBudgetRepository, never()).save(any());
    }

    @Test
    void createBudget_WithNonNullFields_CreatesBudgetAndReturnsDto() {
        when(internalBudgetRepository.existsByBudgetType(BudgetType.GLOBAL)).thenReturn(false);
        when(internalBudgetRepository.save(any(InternalBudget.class))).thenAnswer(invocation -> {
            InternalBudget saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(budgetMapper.toDto(any(InternalBudget.class))).thenReturn(fullDto);

        BudgetDto result = budgetService.createBudget(fullDto);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(fullDto.id(), result.id()),
            () -> assertEquals(fullDto.totalAmount(), result.totalAmount()),
            () -> assertEquals(fullDto.safetyBuffer(), result.safetyBuffer())
        );
        verify(internalBudgetRepository).save(any(InternalBudget.class));
    }

    @Test
    void createBudget_WithNullFields_CreatesBudgetWithZeroAmounts() {
        when(internalBudgetRepository.existsByBudgetType(BudgetType.GLOBAL)).thenReturn(false);
        when(internalBudgetRepository.save(any(InternalBudget.class))).thenAnswer(invocation -> {
            InternalBudget saved = invocation.getArgument(0);
            saved.setId(1L);
            // Verify inside save that nulls are mapped to BigDecimal.ZERO
            assertAll(
                () -> assertEquals(BigDecimal.ZERO, saved.getTotalAmount()),
                () -> assertEquals(BigDecimal.ZERO, saved.getSafetyBuffer())
            );
            return saved;
        });
        when(budgetMapper.toDto(any(InternalBudget.class))).thenReturn(new BudgetDto(1L, 0.0, 0.0));

        BudgetDto result = budgetService.createBudget(nullFieldsDto);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(0.0, result.totalAmount()),
            () -> assertEquals(0.0, result.safetyBuffer())
        );
        verify(internalBudgetRepository).save(any(InternalBudget.class));
    }

    @Test
    void editBudget_GlobalBudgetNotFound_ThrowsEntityNotFoundException() {
        when(internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> budgetService.editBudget(fullDto));
        verify(internalBudgetRepository, never()).save(any());
    }

    @Test
    void editBudget_WithNonNullFields_UpdatesAndReturnsDto() {
        when(internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)).thenReturn(Optional.of(globalBudget));
        when(internalBudgetRepository.save(any(InternalBudget.class))).thenAnswer(invocation -> {
            InternalBudget saved = invocation.getArgument(0);
            assertEquals(BigDecimal.valueOf(2000.0), saved.getTotalAmount());
            assertEquals(BigDecimal.valueOf(100.0), saved.getSafetyBuffer());
            return saved;
        });
        BudgetDto updatedDto = new BudgetDto(1L, 2000.0, 100.0);
        when(budgetMapper.toDto(any(InternalBudget.class))).thenReturn(updatedDto);

        BudgetDto result = budgetService.editBudget(updatedDto);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(2000.0, result.totalAmount()),
            () -> assertEquals(100.0, result.safetyBuffer())
        );
        verify(internalBudgetRepository).save(any(InternalBudget.class));
    }

    @Test
    void editBudget_WithNullFields_DoesNotOverwriteExistingValues() {
        when(internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)).thenReturn(Optional.of(globalBudget));
        when(internalBudgetRepository.save(any(InternalBudget.class))).thenAnswer(invocation -> {
            InternalBudget saved = invocation.getArgument(0);
            // Verify that the existing non-null values remain untouched
            assertEquals(BigDecimal.valueOf(1000.0), saved.getTotalAmount());
            assertEquals(BigDecimal.valueOf(50.0), saved.getSafetyBuffer());
            return saved;
        });
        when(budgetMapper.toDto(any(InternalBudget.class))).thenReturn(fullDto);

        BudgetDto result = budgetService.editBudget(nullFieldsDto);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(1000.0, result.totalAmount()),
            () -> assertEquals(50.0, result.safetyBuffer())
        );
        verify(internalBudgetRepository).save(any(InternalBudget.class));
    }
}
