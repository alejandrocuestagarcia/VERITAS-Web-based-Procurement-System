package com.veritas.backend.user.dto;

import com.veritas.backend.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreationRequestDto(
        @NotBlank
        @Email
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,

        @NotBlank
        @Size(max = 120, message = "Name must be at most 120 characters")
        String name,

        @NotBlank
        @Size(max = 72, message = "Password must be at most 72 characters")
        String password,

        @NotNull UserRole role,
        @Schema(description = "Required only for REQUESTER role")
        Long teamId,
        @Schema(description = "Required only for PROCUREMENT_OFFICER role")
        Long departmentId,
        @Schema(description = "Only applicable for REQUESTER role")
        boolean promoteToTeamLeader) {
}
