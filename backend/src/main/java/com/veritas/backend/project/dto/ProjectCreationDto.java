package com.veritas.backend.project.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectCreationDto(
        @NotBlank String name,
        @NotBlank String projectKey,
        @NotNull Long teamId,
        @FutureOrPresent LocalDate startDate,
        @FutureOrPresent LocalDate endDate,
        @NotNull @Positive BigDecimal budget
) {}
