package com.veritas.backend.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.Positive;

public record ProjectEditDto (
    String name,
    @Positive BigDecimal budget,
    LocalDate startDate,
    LocalDate endDate
){}
