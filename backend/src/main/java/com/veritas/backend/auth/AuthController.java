package com.veritas.backend.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth Module", description = "Secure authentication for general users")
public class AuthController {

    @Operation(summary = "Login", description = "Login as a user and receive token.")
    @PostMapping("/login")
    public String login() {
        return "Successfully logged in";
    }

    @Operation(summary = "Logout", description = "Logout as a user and invalidate token.")
    @PostMapping("/logout")
    public String logout() {
        return "Successfully logged out";
    }

    @Operation(summary = "Reset password", description = "Reset password as user and receive reset email.")
    @PostMapping("/passwordreset")
    public String resetPassword() {
        return "Successfully requested password reset";
    }
}
