package com.veritas.backend.team.dto;

import com.veritas.backend.common.model.Department;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class TeamEditDto {
    @Size(max = 120, message = "Team name must be at most 120 characters")
    private String name;

    @Size(max = 500, message = "Team description must be at most 500 characters")
    private String description;

    private Department department;

    @Positive(message = "Leader id must be positive")
    private Long leaderId;

    private Boolean clearLeader;

    private List<@Positive(message = "Member id must be positive") Long> memberIds;
}
