package com.veritas.backend.team.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeamEditDto {
    @Size(max = 120, message = "Team name must be at most 120 characters")
    private String name;
}
