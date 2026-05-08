package com.veritas.backend.user.dto;

import com.veritas.backend.user.entity.UserRole;
import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String name,
        String email,
        boolean active,
        UserRole role,
        String teamName,
        LocalDateTime createdAt
) {}
