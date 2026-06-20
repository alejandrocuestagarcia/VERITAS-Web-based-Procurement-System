package com.veritas.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
    @NotBlank
    @Size(max = 254, message = "Email must be at most 254 characters")
    String email,

    @NotBlank
    @Size(max = 120, message = "Password must be at most 120 characters")
    String password
) {}
