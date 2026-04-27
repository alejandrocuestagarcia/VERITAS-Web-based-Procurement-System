package com.veritas.backend.auth.controller;

import com.veritas.backend.auth.dto.AuthResponseDto;
import com.veritas.backend.auth.dto.LoginRequestDto;
import com.veritas.backend.auth.dto.RefreshTokenDto;
import com.veritas.backend.auth.service.AuthService;
import com.veritas.backend.config.annotations.IsAdministrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @Operation(summary = "Refresh Token", description = "Get a new access token.")
    @PostMapping(path = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuthResponseDto refresh(@RequestBody RefreshTokenDto refreshToken) {
        return authService.refreshToken(refreshToken);
    }

    @Operation(summary = "Logout", description = "Logout as a user and invalidate token.")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody RefreshTokenDto refreshToken) {
        authService.logout(refreshToken);
    }

    @Operation(summary = "Reset password", description = "Reset password as user and receive reset email.")
    @PostMapping("/passwordreset")
    public AuthResponseDto resetPassword() {
        return new AuthResponseDto(null, null, null, false);
    }

    @Operation(summary = "Admin Password Reset", description = "Allows an admin to set a temporary password for a user.")
    @PostMapping("/admin/reset-password/{id}")
    @IsAdministrator
    public ResponseEntity<String> adminResetPassword(@PathVariable Long id, @RequestBody String tempPassword) {
        authService.adminResetPassword(id, tempPassword);
        return ResponseEntity.ok("Password reset successful");
    }

    @Operation(summary = "Complete Password Change", description = "Finalizes the mandatory password reset process for a user.")
    @PostMapping("/complete-password-change")
    public ResponseEntity<String> completePasswordChange(@RequestBody String newPassword) {
        authService.completePasswordChange(newPassword);
        return ResponseEntity.ok("Password reset successful");
    }
}
