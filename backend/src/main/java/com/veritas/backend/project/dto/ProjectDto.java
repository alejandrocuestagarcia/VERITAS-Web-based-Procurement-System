package com.veritas.backend.project.dto;


import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectDto (
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budget,
        String teamName
) {}
