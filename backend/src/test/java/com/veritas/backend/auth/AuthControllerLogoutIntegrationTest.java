package com.veritas.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.auth.dto.LoginRequestDto;
import com.veritas.backend.auth.dto.RefreshTokenDto;
import com.veritas.backend.auth.repository.RefreshTokenRepository;
import com.veritas.backend.common.model.Department;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerLogoutIntegrationTest extends BaseDBIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuditLogRepository auditLogRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    ObjectMapper objectMapper;

    String refreshToken;

    @BeforeEach
    void setup() throws Exception {
        auditLogRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .name("Test Requester")
                .email("requester@test.com")
                .passwordHash(encoder.encode("password123"))
                .role(UserRole.REQUESTER)
                .department(Department.IT)
                .isActive(true)
                .build());

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto("requester@test.com", "password123"))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        refreshToken = objectMapper.readTree(response).get("refreshToken").asText();
    }

    @Test
    void Logout_ValidToken_ReturnsNoContentAndDeletesToken() throws Exception {
        assertThat(refreshTokenRepository.findByToken(refreshToken)).isPresent();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenDto(refreshToken))))
                .andExpect(status().isNoContent());

        assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
    }

    @Test
    void Logout_InvalidToken_ReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenDto("non-existent-token-123"))))
                .andExpect(status().isNoContent());
    }
}
