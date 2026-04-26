package com.veritas.backend.team.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.veritas.backend.user.dto.UserDto;
import lombok.Data;

@Data
public class TeamDto {
    private Long id;
    private String name;
    private String description;
    private String department;
    private Long leaderId;
    private List<UserDto> members;
    private Boolean isActive;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
