package com.veritas.backend.user.dto;

import com.veritas.backend.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserEditDto (
        String email,
        String name,
        UserRole role,
        @Schema(description = "Applicable only for REQUESTER role")
        Long teamId,
        @Schema(description = "Applicable only for PROCUREMENT_OFFICER role")
        Long departmentId,
        @Schema(description = "Applicable only for REQUESTER role")
        Boolean isTeamLeader
) { }
