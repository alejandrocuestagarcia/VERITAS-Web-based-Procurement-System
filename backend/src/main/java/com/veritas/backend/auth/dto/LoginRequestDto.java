package com.veritas.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(@NotBlank String email, @NotBlank String password) {}
