package com.veritas.backend.budget.service.impl;

import com.veritas.backend.budget.dto.BudgetDashboardDto;
import com.veritas.backend.budget.dto.BudgetDto;
import com.veritas.backend.budget.dto.DepartmentDashboardBudgetDto;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.mapper.BudgetMapper;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.budget.service.BudgetService;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veritas.backend.requisition.repository.InvoiceRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private static final double DEFAULT_FISCAL_RUNWAY_WEEKS = 52.0;

    private final InternalBudgetRepository internalBudgetRepository;
    private final DepartmentRepository departmentRepository;
    private final InvoiceRepository invoiceRepository;

    private final BudgetMapper budgetMapper;

    private record TargetSpendDetails(double runningRemaining, double actSpend, double totalBudg) {}

    @Override
    @Transactional(readOnly = true)
    public BudgetDashboardDto getFinanceDashboard(Long departmentId) {
        int year = LocalDate.now().getYear();
        InternalBudget globalBudget = internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL).orElse(null);

        boolean exists = globalBudget != null;
        double totalBudget = globalBudget != null && globalBudget.getTotalAmount() != null ? globalBudget.getTotalAmount().doubleValue() : 0.0;
        double committedFunds = globalBudget != null && globalBudget.getCommittedSpend() != null ? globalBudget.getCommittedSpend().doubleValue() : 0.0;
        double actualSpend = globalBudget != null && globalBudget.getActualSpend() != null ? globalBudget.getActualSpend().doubleValue() : 0.0;
        double safetyBuffer = globalBudget != null && globalBudget.getSafetyBuffer() != null ? globalBudget.getSafetyBuffer().doubleValue() : 0.0;

        List<DepartmentDashboardBudgetDto> department = getDepartmentBudgetStats();

        TargetSpendDetails spendDetails = getTargetSpendDetails(departmentId, globalBudget, actualSpend, totalBudget);
        List<Double> remainingBudget = calculateBurndownData(year, departmentId, spendDetails.runningRemaining());

        double projBurn = calculateProjectedBurn(spendDetails.actSpend(), year);
        double remaining = spendDetails.totalBudg() - spendDetails.actSpend();
        double runway = calculateFiscalRunway(remaining, projBurn);

        return new BudgetDashboardDto(
                totalBudget,
                committedFunds,
                actualSpend,
                safetyBuffer,
                exists,
                remainingBudget,
                department,
                projBurn,
                runway
        );
    }

    private List<DepartmentDashboardBudgetDto> getDepartmentBudgetStats() {
        return departmentRepository.findAll().stream()
                .map(dept -> new DepartmentDashboardBudgetDto(
                        dept.getName(),
                        dept.getInternalBudget() != null && dept.getInternalBudget().getTotalAmount() != null ? dept.getInternalBudget().getTotalAmount().doubleValue() : 0.0,
                        dept.getInternalBudget() != null && dept.getInternalBudget().getActualSpend() != null ? dept.getInternalBudget().getActualSpend().doubleValue() : 0.0,
                        dept.getInternalBudget() != null && dept.getInternalBudget().getCommittedSpend() != null ? dept.getInternalBudget().getCommittedSpend().doubleValue() : 0.0,
                        dept.getInternalBudget() != null && dept.getInternalBudget().getSafetyBuffer() != null ? dept.getInternalBudget().getSafetyBuffer().doubleValue() : 0.0
                ))
                .collect(Collectors.toList());
    }

    private TargetSpendDetails getTargetSpendDetails(Long departmentId, InternalBudget globalBudget, double globalActualSpend, double globalTotalBudget) {
        if (departmentId != null) {
            Department dept = departmentRepository.findById(departmentId).orElse(null);
            if (dept != null && dept.getInternalBudget() != null) {
                double total = dept.getInternalBudget().getTotalAmount() != null ? dept.getInternalBudget().getTotalAmount().doubleValue() : 0.0;
                double actual = dept.getInternalBudget().getActualSpend() != null ? dept.getInternalBudget().getActualSpend().doubleValue() : 0.0;
                return new TargetSpendDetails(total, actual, total);
            }
            return new TargetSpendDetails(0.0, 0.0, 0.0);
        } else {
            double remaining = globalBudget != null && globalBudget.getTotalAmount() != null ? globalBudget.getTotalAmount().doubleValue() : 0.0;
            return new TargetSpendDetails(remaining, globalActualSpend, globalTotalBudget);
        }
    }

    private List<Double> calculateBurndownData(int year, Long departmentId, double runningRemaining) {
        List<Double> monthlySpend = new ArrayList<>(Collections.nCopies(12, 0.0));
        List<Object[]> queryResults;

        if (departmentId != null) {
            queryResults = invoiceRepository.findActualMonthlySpendByDepartment(year, departmentId);
        } else {
            queryResults = invoiceRepository.findActualMonthlySpend(year);
        }

        for (Object[] row : queryResults) {
            int monthIndex = ((Number) row[0]).intValue() - 1;
            double total = ((Number) row[1]).doubleValue();
            monthlySpend.set(monthIndex, total);
        }

        List<Double> remainingBudget = new ArrayList<>();
        int currentMonth = LocalDate.now().getMonthValue();

        double currentRemaining = runningRemaining;
        for (int i = 0; i < 12; i++) {
            if (i < currentMonth) {
                currentRemaining -= monthlySpend.get(i);
                remainingBudget.add(currentRemaining);
            } else {
                remainingBudget.add(null);
            }
        }
        return remainingBudget;
    }

    private double calculateProjectedBurn(double actSpend, int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate today = LocalDate.now();
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startOfYear, today);
        double elapsedWeeks = Math.max(1.0, (double) daysBetween / 7.0);

        return elapsedWeeks > 0.0 ? actSpend / elapsedWeeks : 0.0;
    }

    private double calculateFiscalRunway(double remaining, double projBurn) {
        return projBurn > 0.0 ? remaining / projBurn : DEFAULT_FISCAL_RUNWAY_WEEKS;
    }

    @Override
    @Transactional
    public BudgetDto createBudget(BudgetDto budgetDto) {
        if (internalBudgetRepository.existsByBudgetType(BudgetType.GLOBAL)) {
            throw new EntityExistsException("Global budget already exists");
        }

        InternalBudget budget = new InternalBudget();
        budget.setBudgetName("Global Budget");
        budget.setBudgetType(BudgetType.GLOBAL);
        if (budgetDto.totalAmount() != null) {
            budget.setTotalAmount(BigDecimal.valueOf(budgetDto.totalAmount()));
        } else {
            budget.setTotalAmount(BigDecimal.ZERO);
        }
        if (budgetDto.safetyBuffer() != null) {
            budget.setSafetyBuffer(BigDecimal.valueOf(budgetDto.safetyBuffer()));
        } else {
            budget.setSafetyBuffer(BigDecimal.ZERO);
        }

        InternalBudget saved = internalBudgetRepository.save(budget);
        return budgetMapper.toDto(saved);
    }

    @Override
    @Transactional
    public BudgetDto editBudget(BudgetDto budgetDto) {
        InternalBudget budget = internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL)
                .orElseThrow(() -> new EntityNotFoundException("Global budget not found"));

        if (budgetDto.totalAmount() != null) {
            BigDecimal newTotal = BigDecimal.valueOf(budgetDto.totalAmount());
            if (newTotal.compareTo(budget.getTotalAmount()) < 0) {
                BigDecimal departmentsBudgetSum = departmentRepository.findAll().stream()
                        .map(d -> d.getInternalBudget() != null ? d.getInternalBudget().getTotalAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (newTotal.compareTo(departmentsBudgetSum) < 0) {
                    DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.GERMANY));
                    throw new IllegalArgumentException("New global budget of " + df.format(newTotal)
                            + "€ is less than the sum of its department budgets " + df.format(departmentsBudgetSum) + "€");
                }
            }
            budget.setTotalAmount(newTotal);
        }
        if (budgetDto.safetyBuffer() != null) {
            budget.setSafetyBuffer(BigDecimal.valueOf(budgetDto.safetyBuffer()));
        }

        InternalBudget saved = internalBudgetRepository.save(budget);
        return budgetMapper.toDto(saved);
    }
}
