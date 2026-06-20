package com.veritas.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenDto(
    @NotBlank
    @Size(max = 500, message = "Refresh token must be at most 500 characters")
    String refreshToken
) {}
