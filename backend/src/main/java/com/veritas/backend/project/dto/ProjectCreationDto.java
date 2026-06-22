package com.veritas.backend.project.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectCreationDto(
        @NotBlank
        @Size(max = 120, message = "Project name must be at most 120 characters")
        String name,

        @NotBlank
        @Size(max = 120, message = "Project key must be at most 120 characters")
        String projectKey,

        @NotNull Long teamId,
        @FutureOrPresent LocalDate startDate,
        @FutureOrPresent LocalDate endDate,
        @NotNull @Positive BigDecimal budget
) {}
