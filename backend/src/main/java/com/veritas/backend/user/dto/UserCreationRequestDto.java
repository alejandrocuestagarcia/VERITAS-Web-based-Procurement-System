package com.veritas.backend.user.dto;

import com.veritas.backend.user.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreationRequestDto(@NotBlank @Email String email, @NotBlank String name, @NotBlank String password,
        @NotNull UserRole role, @NotNull Long teamId, boolean promoteToTeamLeader) {
}
