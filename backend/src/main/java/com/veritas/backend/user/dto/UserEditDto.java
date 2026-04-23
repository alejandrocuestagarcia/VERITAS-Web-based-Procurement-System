package com.veritas.backend.user.dto;

public record UserEditDto (
        String email,
        String name,
        Long teamId,
        Boolean isTeamLeader
) { }
