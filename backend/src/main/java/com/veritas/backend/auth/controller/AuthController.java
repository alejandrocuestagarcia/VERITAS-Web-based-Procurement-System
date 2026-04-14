package com.veritas.backend.auth.controller;

import com.veritas.backend.auth.dto.AuthResponseDto;
import com.veritas.backend.auth.dto.LoginRequestDto;
import com.veritas.backend.auth.dto.RefreshTokenDto;
import com.veritas.backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth Module", description = "Secure authentication for general users")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login", description = "Login as a user and receive token.")
    @PostMapping(path = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuthResponseDto login(@RequestBody LoginRequestDto loginRequest) {
        return authService.login(loginRequest);
    }

    @Operation(summary = "Refresh Token", description = "Get a new access token.")
    @PostMapping("/refresh")
    public AuthResponseDto refresh(@RequestBody RefreshTokenDto refreshToken) {
        return authService.refreshToken(refreshToken);
    }

    @Operation(summary = "Logout", description = "Logout as a user and invalidate token.")
    @PostMapping("/logout")
    public AuthResponseDto logout() {
        return new AuthResponseDto(null, null, null);

    }

    @Operation(summary = "Reset password", description = "Reset password as user and receive reset email.")
    @PostMapping("/passwordreset")
    public AuthResponseDto resetPassword() {
        return new AuthResponseDto(null, null, null);
    }
}
