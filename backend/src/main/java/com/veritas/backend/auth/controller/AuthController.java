package com.veritas.backend.auth.controller;

import com.veritas.backend.auth.dto.AuthResponseDto;
import com.veritas.backend.auth.dto.LoginRequestDto;
import com.veritas.backend.auth.dto.RefreshTokenDto;
import com.veritas.backend.auth.service.AuthService;
import com.veritas.backend.config.annotations.IsAdministrator;
import com.veritas.backend.config.annotations.IsRequester;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
        return new AuthResponseDto(null, null, null, false);
    }

    @Operation(summary = "Admin Password Reset", description = "Allows an admin to set a temporary password for a user.")
    @PostMapping("/admin/reset-password/{id}")
    @IsAdministrator
    public ResponseEntity<String> adminResetPassword(@PathVariable Long id, @RequestBody String tempPassword) {
        log.info("POST /auth/admin/reset-password/{}", id);
        authService.adminResetPassword(id, tempPassword);
        return ResponseEntity.ok("Password reset successful");
    }

    @Operation(summary = "Complete Password Change", description = "Finalizes the mandatory password reset process for a user.")
    @PostMapping("/complete-password-change")
    @IsRequester
    public ResponseEntity<String> completePasswordChange(@RequestBody String newPassword) {
        log.info("POST /auth/complete-password-change");
        authService.completePasswordChange(newPassword);
        return ResponseEntity.ok("Password reset successful");
    }
}

