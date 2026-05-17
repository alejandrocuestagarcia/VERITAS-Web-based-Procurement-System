package com.veritas.backend.user.dto;

import com.veritas.backend.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreationRequestDto(
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotBlank String password,
        @NotNull UserRole role,
        @Schema(description = "Required only for REQUESTER role")
        Long teamId,
        @Schema(description = "Required only for PROCUREMENT_OFFICER role")
        Long departmentId,
        @Schema(description = "Only applicable for REQUESTER role")
        boolean promoteToTeamLeader) {
}
