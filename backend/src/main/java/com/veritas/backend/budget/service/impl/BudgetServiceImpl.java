package com.veritas.backend.budget.service.impl;

import com.veritas.backend.budget.dto.BudgetDto;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.mapper.BudgetMapper;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.budget.service.BudgetService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final InternalBudgetRepository internalBudgetRepository;
    private final BudgetMapper budgetMapper;

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
            budget.setTotalAmount(BigDecimal.valueOf(budgetDto.totalAmount()));
        }
        if (budgetDto.safetyBuffer() != null) {
            budget.setSafetyBuffer(BigDecimal.valueOf(budgetDto.safetyBuffer()));
        }

        InternalBudget saved = internalBudgetRepository.save(budget);
        return budgetMapper.toDto(saved);
    }
}
