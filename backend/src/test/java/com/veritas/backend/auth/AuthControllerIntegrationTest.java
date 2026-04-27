package com.veritas.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.auth.dto.LoginRequestDto;
import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends BaseDBIntegrationTest {
    private static final String CORRECT_EMAIL = "test@veritas.com", FALSE_EMAIL = "unknown@veritas.com",
            CORRECT_PASSWORD = "password123", FALSE_PASSWORD = "wrongpassword";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtService jwtService;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    com.veritas.backend.auth.repository.RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setup() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail(CORRECT_EMAIL);
        user.setName("Test User");
        user.setPasswordHash(encoder.encode(CORRECT_PASSWORD));
        user.setRole(UserRole.REQUESTER);
        userRepository.save(user);
    }

    @Test
    void Login_ValidCredentials_ReturnsTokensAndRole() throws Exception {
        LoginRequestDto request = new LoginRequestDto(CORRECT_EMAIL, CORRECT_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value(UserRole.REQUESTER.name()));
    }

    @Test
    void Login_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        LoginRequestDto request = new LoginRequestDto(CORRECT_EMAIL, FALSE_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void Login_UserNotFound_ReturnsUnauthorized() throws Exception {
        LoginRequestDto request = new LoginRequestDto(FALSE_EMAIL, CORRECT_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
