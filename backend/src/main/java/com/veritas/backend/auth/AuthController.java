package com.veritas.backend.auth;

import com.veritas.backend.auth.dto.AuthResponseDto;
import com.veritas.backend.auth.dto.LoginRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth Module", description = "Secure authentication for general users")
public class AuthController {

    @Operation(summary = "Login", description = "Login as a user and receive token.")
    @PostMapping("/login")
    public AuthResponseDto login(@RequestBody LoginRequestDto loginRequest) {
        AuthResponseDto response = new AuthResponseDto();
        response.setMessage("Successfully logged in");
        response.setToken("dummy-token");
        return response;
    }

    @Operation(summary = "Logout", description = "Logout as a user and invalidate token.")
    @PostMapping("/logout")
    public AuthResponseDto logout() {
        AuthResponseDto response = new AuthResponseDto();
        response.setMessage("Successfully logged out");
        return response;
    }

    @Operation(summary = "Reset password", description = "Reset password as user and receive reset email.")
    @PostMapping("/passwordreset")
    public AuthResponseDto resetPassword() {
        AuthResponseDto response = new AuthResponseDto();
        response.setMessage("Successfully requested password reset");
        return response;
    }
}
