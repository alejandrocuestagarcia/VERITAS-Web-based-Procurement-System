package com.veritas.backend.user.dto;

import com.veritas.backend.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UserEditDto (
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,

        @Size(max = 120, message = "Name must be at most 120 characters")
        String name,

        UserRole role,
        @Schema(description = "Applicable only for REQUESTER role")
        Long teamId,
        @Schema(description = "Applicable only for PROCUREMENT_OFFICER role")
        Long departmentId,
        @Schema(description = "Applicable only for REQUESTER role")
        Boolean isTeamLeader
) { }
