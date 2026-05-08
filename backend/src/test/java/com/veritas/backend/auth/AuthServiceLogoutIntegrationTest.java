package com.veritas.backend.auth;

import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.auth.dto.LoginRequestDto;
import com.veritas.backend.auth.dto.RefreshTokenDto;
import com.veritas.backend.auth.repository.RefreshTokenRepository;
import com.veritas.backend.auth.service.AuthService;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class AuthServiceLogoutIntegrationTest extends BaseDBIntegrationTest {
    @Autowired
    AuthService authService;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuditLogRepository auditLogRepository;

    @Autowired
    PasswordEncoder encoder;

    String refreshToken;

    @BeforeEach
    void setup() {
        auditLogRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .name("Test Requester")
                .email("requester@test.com")
                .passwordHash(encoder.encode("password123"))
                .role(UserRole.REQUESTER)
                .isActive(true)
                .build());

        var response = authService.login(new LoginRequestDto("requester@test.com", "password123"));
        refreshToken = response.refreshToken();
    }

    @Test
    void Logout_ValidToken_DeletesRefreshTokenFromDatabase() {
        assertThat(refreshTokenRepository.findByToken(refreshToken)).isPresent();

        authService.logout(new RefreshTokenDto(refreshToken));

        assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
    }

    @Test
    void Logout_NonExistentToken_DoesNotThrowException() {
        assertDoesNotThrow(() -> authService.logout(new RefreshTokenDto("non-existent-token-123")));
    }

    @Test
    void Logout_ValidToken_PreventsTokenReuse() {
        authService.logout(new RefreshTokenDto(refreshToken));

        assertThrows(RuntimeException.class, () -> authService.refreshToken(new RefreshTokenDto(refreshToken)));
    }
}
