package com.veritas.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequestDto(
    @NotBlank
    @Email
    @Size(max = 254, message = "Email must be at most 254 characters")
    String email
) {}
