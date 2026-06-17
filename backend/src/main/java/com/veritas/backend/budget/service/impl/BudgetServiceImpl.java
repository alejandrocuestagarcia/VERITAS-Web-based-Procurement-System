package com.veritas.backend.budget.service.impl;

import com.veritas.backend.budget.dto.BudgetDashboardDto;
import com.veritas.backend.budget.dto.BudgetDto;
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

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final InternalBudgetRepository internalBudgetRepository;
    private final DepartmentRepository departmentRepository;
    private final InvoiceRepository invoiceRepository;

    private final BudgetMapper budgetMapper;

    @Override
    @Transactional(readOnly = true)
    public BudgetDashboardDto getFinanceDashboard(Long departmentId) {

        int year = LocalDate.now().getYear();

        InternalBudget globalBudget = internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL).orElse(null);

        BudgetDashboardDto dto = new BudgetDashboardDto();
        if (globalBudget != null) {
            dto.setExists(true);
            dto.setTotalBudget(globalBudget.getTotalAmount() != null ? globalBudget.getTotalAmount().doubleValue() : 0.0);
            dto.setCommittedFunds(globalBudget.getCommittedSpend() != null ? globalBudget.getCommittedSpend().doubleValue() : 0.0);
            dto.setActualSpend(globalBudget.getActualSpend() != null ? globalBudget.getActualSpend().doubleValue() : 0.0);
            dto.setSafetyBuffer(globalBudget.getSafetyBuffer() != null ? globalBudget.getSafetyBuffer().doubleValue() : 0.0);
        } else {
            dto.setExists(false);
            dto.setTotalBudget(0.0);
            dto.setCommittedFunds(0.0);
            dto.setActualSpend(0.0);
            dto.setSafetyBuffer(0.0);
        }

        List<Object> department = departmentRepository.findAll().stream()
                .map(dept -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("department", dept.getName());
                    map.put("budget", dept.getInternalBudget() != null && dept.getInternalBudget().getTotalAmount() != null ? dept.getInternalBudget().getTotalAmount().doubleValue() : 0.0);
                    map.put("spent", dept.getInternalBudget() != null && dept.getInternalBudget().getActualSpend() != null ? dept.getInternalBudget().getActualSpend().doubleValue() : 0.0);
                    map.put("committed", dept.getInternalBudget() != null && dept.getInternalBudget().getCommittedSpend() != null ? dept.getInternalBudget().getCommittedSpend().doubleValue() : 0.0);
                    map.put("safetyBuffer", dept.getInternalBudget() != null && dept.getInternalBudget().getSafetyBuffer() != null ? dept.getInternalBudget().getSafetyBuffer().doubleValue() : 0.0);
                    return map;
                })
                .collect(Collectors.toList());

        List<Double> monthlySpend = new ArrayList<>(Collections.nCopies(12, 0.0));
        List<Object[]> queryResults;
        double runningRemaining = 0.0;
        double actSpend = 0.0;
        double totalBudg = 0.0;

        if (departmentId != null) {
            queryResults = invoiceRepository.findActualMonthlySpendByDepartment(year, departmentId);
            Department dept = departmentRepository.findById(departmentId).orElse(null);
            if (dept != null && dept.getInternalBudget() != null) {
                runningRemaining = dept.getInternalBudget().getTotalAmount() != null ? dept.getInternalBudget().getTotalAmount().doubleValue() : 0.0;
                actSpend = dept.getInternalBudget().getActualSpend() != null ? dept.getInternalBudget().getActualSpend().doubleValue() : 0.0;
                totalBudg = runningRemaining;
            }
        } else {
            queryResults = invoiceRepository.findActualMonthlySpend(year);
            runningRemaining = globalBudget != null && globalBudget.getTotalAmount() != null ? globalBudget.getTotalAmount().doubleValue() : 0.0;
            actSpend = dto.getActualSpend() != null ? dto.getActualSpend() : 0.0;
            totalBudg = dto.getTotalBudget() != null ? dto.getTotalBudget() : 0.0;
        }

        for (Object[] row : queryResults) {
            int monthIndex = ((Number) row[0]).intValue() - 1;
            double total = ((Number) row[1]).doubleValue();
            monthlySpend.set(monthIndex, total);
        }

        List<Double> remainingBudget = new ArrayList<>();
        int currentMonth = LocalDate.now().getMonthValue();

        for (int i = 0; i < 12; i++) {
                if (i < currentMonth) {
                    runningRemaining -= monthlySpend.get(i);
                    remainingBudget.add(runningRemaining);
                } else {
                    remainingBudget.add(null);
                }
        }

        dto.setBurndownData(remainingBudget);
        dto.setDepartmentData(department);

        double elapsedWeeks;
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate today = LocalDate.now();
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startOfYear, today);
        elapsedWeeks = Math.max(1.0, (double) daysBetween / 7.0);

        double projBurn = elapsedWeeks > 0.0 ? actSpend / elapsedWeeks : 0.0;
        double remaining = totalBudg - actSpend;
        double runway = projBurn > 0.0 ? remaining / projBurn : 52.0;

        dto.setProjectedBurn(projBurn);
        dto.setFiscalRunway(runway);

        return dto;
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
                    throw new IllegalArgumentException("New global budget of " + newTotal
                            + " is less than the sum of its department budgets (" + departmentsBudgetSum + ")");
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
