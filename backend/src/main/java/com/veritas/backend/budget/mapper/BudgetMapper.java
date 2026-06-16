package com.veritas.backend.budget.mapper;

import com.veritas.backend.budget.dto.BudgetDto;
import com.veritas.backend.budget.entity.InternalBudget;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface BudgetMapper {
    BudgetDto toDto(InternalBudget budget);
}
