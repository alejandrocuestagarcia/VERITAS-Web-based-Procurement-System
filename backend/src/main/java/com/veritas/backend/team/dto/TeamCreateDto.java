package com.veritas.backend.team.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class TeamCreateDto {
    @NotBlank(message = "Team name is required")
    @Size(max = 120, message = "Team name must be at most 120 characters")
    private String name;

    @NotBlank(message = "Team description is required")
    @Size(max = 500, message = "Team description must be at most 500 characters")
    private String description;

    @NotNull(message = "Department is required")
    private Long departmentId;

    @Positive(message = "Leader id must be positive")
    private Long leaderId;

    @Future(message = "Expiration date must be in the future")
    private LocalDateTime expiresAt;

    private List<@Positive(message = "Member id must be positive") Long> memberIds;
}