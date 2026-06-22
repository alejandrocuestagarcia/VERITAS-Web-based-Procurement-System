package com.veritas.backend.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProjectEditDto (
    @Size(max = 120, message = "Project name must be at most 120 characters")
    String name,
    @Positive BigDecimal budget,
    Long teamId,
    LocalDate startDate,
    LocalDate endDate
){}
