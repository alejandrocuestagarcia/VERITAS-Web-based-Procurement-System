package com.veritas.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmDto(
    @NotBlank
    @Size(max = 120, message = "Token must be at most 120 characters")
    String token,

    @NotBlank
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    String newPassword
) {}
