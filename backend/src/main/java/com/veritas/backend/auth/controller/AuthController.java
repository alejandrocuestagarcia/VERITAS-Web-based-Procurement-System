package com.veritas.backend.auth.controller;

import com.veritas.backend.auth.dto.AuthResponseDto;
import com.veritas.backend.auth.dto.LoginRequestDto;
import com.veritas.backend.auth.dto.RefreshTokenDto;
import com.veritas.backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Module", description = "Secure authentication for general users")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login", description = "Login as a user and receive token.")
    @PostMapping(path = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto loginRequest) {
        log.info("POST /auth/login – email: {}", loginRequest.email());
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @Operation(summary = "Refresh Token", description = "Get a new access token.")
    @PostMapping(path = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuthResponseDto refresh(@RequestBody RefreshTokenDto refreshToken) {
        log.info("POST /auth/refresh");
        return authService.refreshToken(refreshToken);
    }

    @Operation(summary = "Logout", description = "Logout as a user and invalidate token.")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody RefreshTokenDto refreshToken) {
        log.info("POST /auth/logout");
        authService.logout(refreshToken);
    }

    @Operation(summary = "Reset password", description = "Reset password as user and receive reset email.")
    @PostMapping("/passwordreset")
    public AuthResponseDto resetPassword() {
        log.info("POST /auth/passwordreset");
        return new AuthResponseDto(null, null, null);
    }
}

