package com.veritas.backend.auth.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.auth.dto.AuthResponseDto;
import com.veritas.backend.auth.dto.LoginRequestDto;
import com.veritas.backend.auth.dto.PasswordResetConfirmDto;
import com.veritas.backend.auth.dto.PasswordResetRequestDto;
import com.veritas.backend.auth.dto.RefreshTokenDto;
import com.veritas.backend.auth.service.AuthService;
import com.veritas.backend.auth.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void login_ValidRequest_ReturnsOk() throws Exception {
        LoginRequestDto request = new LoginRequestDto("test@veritas.com", "password");
        AuthResponseDto response = new AuthResponseDto("access", "refresh", "ROLE_ADMIN", false);
        when(authService.login(any(LoginRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));

        verify(authService).login(any(LoginRequestDto.class));
    }

    @Test
    void refresh_ValidRequest_ReturnsResponse() throws Exception {
        RefreshTokenDto request = new RefreshTokenDto("refresh_token");
        AuthResponseDto response = new AuthResponseDto("access", "refresh_token", "ROLE_ADMIN", false);
        when(authService.refreshToken(any(RefreshTokenDto.class))).thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"));

        verify(authService).refreshToken(any(RefreshTokenDto.class));
    }

    @Test
    void logout_ValidRequest_ReturnsNoContent() throws Exception {
        RefreshTokenDto request = new RefreshTokenDto("refresh_token");

        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).logout(any(RefreshTokenDto.class));
    }

    @Test
    void requestPasswordReset_ValidRequest_ReturnsNoContent() throws Exception {
        PasswordResetRequestDto request = new PasswordResetRequestDto("test@veritas.com");

        mockMvc.perform(post("/auth/passwordreset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(passwordResetService).requestReset("test@veritas.com");
    }

    @Test
    void confirmPasswordReset_ValidRequest_ReturnsNoContent() throws Exception {
        PasswordResetConfirmDto request = new PasswordResetConfirmDto("token", "new_password");

        mockMvc.perform(post("/auth/passwordreset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(passwordResetService).confirmReset("token", "new_password");
    }

    @Test
    void adminResetPassword_ValidRequest_ReturnsNoContent() throws Exception {
        mockMvc.perform(post("/auth/admin/reset-password/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("tempPassword"))
                .andExpect(status().isNoContent());

        verify(authService).adminResetPassword(1L, "tempPassword");
    }

    @Test
    void completePasswordChange_ValidRequest_ReturnsNoContent() throws Exception {
        mockMvc.perform(post("/auth/complete-password-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content("newPassword"))
                .andExpect(status().isNoContent());

        verify(authService).completePasswordChange("newPassword");
    }
}
