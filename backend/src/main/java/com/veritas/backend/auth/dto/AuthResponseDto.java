package com.veritas.backend.auth.dto;

public record AuthResponseDto(
        String accessToken,
        String refreshToken,
        String role
) {}