package com.veritas.backend.user.dto;

import com.veritas.backend.common.model.Department;
import com.veritas.backend.user.entity.UserRole;

public record UserEditDto (
        String email,
        String name,
        UserRole role,
        Long teamId,
        Department department,
        Boolean isTeamLeader
) { }
