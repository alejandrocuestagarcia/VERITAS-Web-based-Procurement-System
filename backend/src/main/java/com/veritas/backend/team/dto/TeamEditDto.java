package com.veritas.backend.team.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class TeamEditDto {
    @Size(max = 120, message = "Team name must be at most 120 characters")
    private String name;

    @Size(max = 3000, message = "Team description must be at most 3000 characters")
    private String description;

    @Positive(message = "Department id must be positive")
    private Long departmentId;

    @Positive(message = "Leader id must be positive")
    private Long leaderId;

    private Boolean clearLeader;

    private List<@Positive(message = "Member id must be positive") Long> memberIds;
}
