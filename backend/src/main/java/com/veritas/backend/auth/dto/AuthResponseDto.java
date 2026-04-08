package com.veritas.backend.auth.dto;

import lombok.Data;

@Data
public class AuthResponseDto {
    private String message;
    private String token;
}
