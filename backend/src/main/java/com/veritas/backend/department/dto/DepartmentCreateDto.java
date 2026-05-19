package com.veritas.backend.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DepartmentCreateDto(
        @NotBlank(message = "Department name is required") @Size(max = 120, message = "Department name must be at most 120 characters") String name,

        @NotNull @Positive BigDecimal budget) {
}
