package com.veritas.backend.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentDto(
        Long id,
        @NotBlank(message = "Team name is required")
        @Size(max = 120, message = "Team name must be at most 120 characters")
        String name
) {}