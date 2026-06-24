package com.veritas.backend.budget.service;

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
import com.veritas.backend.budget.dto.BudgetDashboardDto;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.requisition.repository.InvoiceRepository;
import java.util.List;
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
    private DepartmentRepository departmentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

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

    @Test
    void editBudget_newTotalLessThanDepartmentsBudgetSum_throwsIllegalArgumentException() {
        when(internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)).thenReturn(Optional.of(globalBudget));
        
        Department dept1 = new Department();
        InternalBudget budget1 = new InternalBudget();
        budget1.setTotalAmount(BigDecimal.valueOf(800.0));
        dept1.setInternalBudget(budget1);

        Department dept2 = new Department();
        InternalBudget budget2 = new InternalBudget();
        budget2.setTotalAmount(BigDecimal.valueOf(300.0));
        dept2.setInternalBudget(budget2);

        when(departmentRepository.findAll()).thenReturn(List.of(dept1, dept2));

        // departments sum = 1100.0, new total = 900.0 (which is < 1100.0)
        BudgetDto updatedDto = new BudgetDto(1L, 900.0, 50.0);

        assertThrows(IllegalArgumentException.class, () -> budgetService.editBudget(updatedDto));
        verify(internalBudgetRepository, never()).save(any());
    }

    @Test
    void editBudget_newTotalGreaterThanOrEqualToDepartmentsBudgetSum_updatesSuccessfully() {
        when(internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)).thenReturn(Optional.of(globalBudget));
        when(internalBudgetRepository.save(any(InternalBudget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Department dept = new Department();
        InternalBudget budget = new InternalBudget();
        budget.setTotalAmount(BigDecimal.valueOf(800.0));
        dept.setInternalBudget(budget);

        when(departmentRepository.findAll()).thenReturn(List.of(dept));

        // departments sum = 800.0, new total = 900.0 (which is >= 800.0)
        BudgetDto updatedDto = new BudgetDto(1L, 900.0, 50.0);
        when(budgetMapper.toDto(any(InternalBudget.class))).thenReturn(updatedDto);

        BudgetDto result = budgetService.editBudget(updatedDto);

        assertNotNull(result);
        assertEquals(900.0, result.totalAmount());
        verify(internalBudgetRepository).save(any(InternalBudget.class));
    }

    @Test
    void getFinanceDashboard_nullGlobalBudget_returnsDashboardWithDefaultZeros() {
        when(internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)).thenReturn(Optional.empty());
        when(departmentRepository.findAll()).thenReturn(List.of());
        when(invoiceRepository.findActualMonthlySpend(anyInt())).thenReturn(List.of());

        BudgetDashboardDto result = budgetService.getFinanceDashboard(null);

        assertAll(
            () -> assertNotNull(result),
            () -> assertFalse(result.exists()),
            () -> assertEquals(0.0, result.totalBudget()),
            () -> assertEquals(0.0, result.committedFunds()),
            () -> assertEquals(0.0, result.actualSpend()),
            () -> assertEquals(0.0, result.safetyBuffer()),
            () -> assertEquals(0.0, result.projectedBurn()),
            () -> assertEquals(52.0, result.fiscalRunway()),
            () -> assertNotNull(result.burndownData()),
            () -> assertEquals(12, result.burndownData().size())
        );
    }

    @Test
    void getFinanceDashboard_withGlobalBudgetAndDepartments_returnsCorrectDashboard() {
        InternalBudget budget = new InternalBudget();
        budget.setTotalAmount(BigDecimal.valueOf(10000.0));
        budget.setCommittedSpend(BigDecimal.valueOf(2000.0));
        budget.setActualSpend(BigDecimal.valueOf(1500.0));
        budget.setSafetyBuffer(BigDecimal.valueOf(1000.0));

        when(internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)).thenReturn(Optional.of(budget));

        Department dept1 = new Department();
        dept1.setName("IT");
        InternalBudget deptBudget1 = new InternalBudget();
        deptBudget1.setTotalAmount(BigDecimal.valueOf(5000.0));
        deptBudget1.setActualSpend(BigDecimal.valueOf(1000.0));
        deptBudget1.setCommittedSpend(BigDecimal.valueOf(500.0));
        deptBudget1.setSafetyBuffer(BigDecimal.valueOf(200.0));
        dept1.setInternalBudget(deptBudget1);

        Department dept2 = new Department();
        dept2.setName("Marketing");
        dept2.setInternalBudget(null); // Triggers null checking path

        when(departmentRepository.findAll()).thenReturn(List.of(dept1, dept2));

        // Mock monthly spend
        List<Object[]> monthlySpend = List.of(
            new Object[]{1, BigDecimal.valueOf(500.0)},
            new Object[]{2, BigDecimal.valueOf(1000.0)}
        );
        when(invoiceRepository.findActualMonthlySpend(anyInt())).thenReturn(monthlySpend);

        BudgetDashboardDto result = budgetService.getFinanceDashboard(null);

        assertAll(
            () -> assertNotNull(result),
            () -> assertTrue(result.exists()),
            () -> assertEquals(10000.0, result.totalBudget()),
            () -> assertEquals(2000.0, result.committedFunds()),
            () -> assertEquals(1500.0, result.actualSpend()),
            () -> assertEquals(1000.0, result.safetyBuffer()),
            () -> assertEquals(2, result.departmentData().size()),
            () -> assertEquals("IT", result.departmentData().get(0).department()),
            () -> assertEquals(5000.0, result.departmentData().get(0).budget()),
            () -> assertEquals("Marketing", result.departmentData().get(1).department()),
            () -> assertEquals(0.0, result.departmentData().get(1).budget())
        );
    }

    @Test
    void getFinanceDashboard_withDepartmentIdFilter_returnsDepartmentDashboard() {
        InternalBudget budget = new InternalBudget();
        budget.setTotalAmount(BigDecimal.valueOf(10000.0));
        budget.setActualSpend(BigDecimal.valueOf(1500.0));
        when(internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)).thenReturn(Optional.of(budget));

        Department dept = new Department();
        dept.setName("IT");
        InternalBudget deptBudget = new InternalBudget();
        deptBudget.setTotalAmount(BigDecimal.valueOf(5000.0));
        deptBudget.setActualSpend(BigDecimal.valueOf(1200.0));
        dept.setInternalBudget(deptBudget);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(departmentRepository.findAll()).thenReturn(List.of(dept));

        List<Object[]> monthlySpend = List.of(
            new Object[]{1, BigDecimal.valueOf(600.0)},
            new Object[]{2, BigDecimal.valueOf(600.0)}
        );
        when(invoiceRepository.findActualMonthlySpendByDepartment(anyInt(), eq(1L))).thenReturn(monthlySpend);

        BudgetDashboardDto result = budgetService.getFinanceDashboard(1L);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(10000.0, result.totalBudget()), // Global values are returned for top-level stats
            () -> assertEquals(1500.0, result.actualSpend()),
            () -> assertEquals(1, result.departmentData().size())
        );
    }

    @Test
    void getFinanceDashboard_withDepartmentIdFilter_departmentNotFound_returnsZeros() {
        when(internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)).thenReturn(Optional.empty());
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());
        when(departmentRepository.findAll()).thenReturn(List.of());
        when(invoiceRepository.findActualMonthlySpendByDepartment(anyInt(), eq(1L))).thenReturn(List.of());

        BudgetDashboardDto result = budgetService.getFinanceDashboard(1L);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(0.0, result.totalBudget()),
            () -> assertEquals(0.0, result.actualSpend())
        );
    }

    @Test
    void getFinanceDashboard_withGlobalBudgetAndDepartmentsHavingNullFields_returnsDashboard() {
        InternalBudget budget = new InternalBudget(); // totalAmount, actualSpend, safetyBuffer are all null
        budget.setCommittedSpend(null);
        budget.setActualSpend(null);
        budget.setSafetyBuffer(null);
        when(internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)).thenReturn(Optional.of(budget));

        Department dept = new Department();
        dept.setName("IT");
        InternalBudget deptBudget = new InternalBudget(); // totalAmount, etc are null
        deptBudget.setCommittedSpend(null);
        deptBudget.setActualSpend(null);
        deptBudget.setSafetyBuffer(null);
        dept.setInternalBudget(deptBudget);

        when(departmentRepository.findAll()).thenReturn(List.of(dept));
        when(invoiceRepository.findActualMonthlySpend(anyInt())).thenReturn(List.of());

        BudgetDashboardDto result = budgetService.getFinanceDashboard(null);

        assertAll(
            () -> assertNotNull(result),
            () -> assertTrue(result.exists()),
            () -> assertEquals(0.0, result.totalBudget()),
            () -> assertEquals(0.0, result.committedFunds()),
            () -> assertEquals(0.0, result.actualSpend()),
            () -> assertEquals(0.0, result.safetyBuffer()),
            () -> assertEquals(1, result.departmentData().size()),
            () -> assertEquals(0.0, result.departmentData().get(0).budget())
        );
    }

    @Test
    void getFinanceDashboard_withDepartmentIdFilter_departmentBudgetFieldsNull_returnsZeros() {
        InternalBudget budget = new InternalBudget();
        budget.setCommittedSpend(null);
        budget.setActualSpend(null);
        budget.setSafetyBuffer(null);
        when(internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)).thenReturn(Optional.of(budget));

        Department dept = new Department();
        dept.setName("IT");
        InternalBudget deptBudget = new InternalBudget(); // null fields
        deptBudget.setCommittedSpend(null);
        deptBudget.setActualSpend(null);
        deptBudget.setSafetyBuffer(null);
        dept.setInternalBudget(deptBudget);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(departmentRepository.findAll()).thenReturn(List.of(dept));
        when(invoiceRepository.findActualMonthlySpendByDepartment(anyInt(), eq(1L))).thenReturn(List.of());

        BudgetDashboardDto result = budgetService.getFinanceDashboard(1L);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(0.0, result.totalBudget()),
            () -> assertEquals(0.0, result.actualSpend())
        );
    }

    @Test
    void getFinanceDashboard_withDepartmentIdFilter_departmentBudgetNull_returnsZeros() {
        InternalBudget budget = new InternalBudget();
        when(internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)).thenReturn(Optional.of(budget));

        Department dept = new Department();
        dept.setName("IT");
        dept.setInternalBudget(null);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(departmentRepository.findAll()).thenReturn(List.of(dept));
        when(invoiceRepository.findActualMonthlySpendByDepartment(anyInt(), eq(1L))).thenReturn(List.of());

        BudgetDashboardDto result = budgetService.getFinanceDashboard(1L);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(0.0, result.totalBudget()),
            () -> assertEquals(0.0, result.actualSpend())
        );
    }

    @Test
    void editBudget_withDepartmentHavingNullBudget_updatesSuccessfully() {
        when(internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)).thenReturn(Optional.of(globalBudget));
        when(internalBudgetRepository.save(any(InternalBudget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Department dept1 = new Department();
        dept1.setInternalBudget(null);

        Department dept2 = new Department();
        InternalBudget budget2 = new InternalBudget();
        budget2.setTotalAmount(BigDecimal.ZERO);
        dept2.setInternalBudget(budget2);

        when(departmentRepository.findAll()).thenReturn(List.of(dept1, dept2));

        BudgetDto updatedDto = new BudgetDto(1L, 900.0, 50.0);
        when(budgetMapper.toDto(any(InternalBudget.class))).thenReturn(updatedDto);

        BudgetDto result = budgetService.editBudget(updatedDto);

        assertNotNull(result);
        assertEquals(900.0, result.totalAmount());
        verify(internalBudgetRepository).save(any(InternalBudget.class));
    }
}

