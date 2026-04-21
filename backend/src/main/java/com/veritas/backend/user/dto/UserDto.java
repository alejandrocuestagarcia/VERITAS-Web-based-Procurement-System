package com.veritas.backend.user.dto;

import com.veritas.backend.user.entity.UserRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

public record UserDto(Long id, String name, String email, UserRole role, String teamName, LocalDateTime createdAt) {
}
