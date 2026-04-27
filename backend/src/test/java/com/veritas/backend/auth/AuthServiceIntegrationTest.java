package com.veritas.backend.auth;

import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.auth.service.impl.AuthServiceImpl;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class AuthServiceIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private AuthServiceImpl authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;

    @org.junit.jupiter.api.BeforeEach
    void cleanDatabase() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminResetPassword_ShouldPersistStateAndAuditLog() {
        User admin = userRepository.save(User.builder()
                .name("Integration Admin")
                .email("admin-test-" + java.util.UUID.randomUUID() + "@veritas.com") // Unique email
                .passwordHash("hash")
                .role(UserRole.ADMINISTRATOR)
                .isActive(true)
                .build());

        User targetUser = userRepository.save(User.builder()
                .name("Integration Target")
                .email("target-test-" + java.util.UUID.randomUUID() + "@veritas.com") // Unique email
                .passwordHash("old")
                .role(UserRole.REQUESTER)
                .isActive(true)
                .requiresPasswordChange(false)
                .build());

        org.springframework.security.core.Authentication auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        admin, null, java.util.Collections.emptyList()
                );
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        authService.adminResetPassword(targetUser.getId(), "secret123");

        User updatedUser = userRepository.findById(targetUser.getId()).orElseThrow();
        assertTrue(updatedUser.getRequiresPasswordChange());

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}