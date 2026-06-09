package com.veritas.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.veritas.backend.audit.service.AuditService;
import com.veritas.backend.auth.entity.PasswordResetToken;
import com.veritas.backend.auth.repository.PasswordResetTokenRepository;
import com.veritas.backend.auth.service.impl.PasswordResetServiceImpl;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.veritas.backend.mail.MailService;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// AI-GENERATED
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PasswordResetServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "expirationHours", 24L);
        ReflectionTestUtils.setField(passwordResetService, "resetLinkBase", "http://localhost:4200/reset-password");

        ReflectionTestUtils.setField(passwordResetService, "subject", "Reset your Veritas password");
    }

    @Test
    void RequestReset_UserFound_SendsEmailAndPersistsToken() {
        User user = new User();
        user.setEmail("user@veritas.com");

        when(userRepository.findByEmail("user@veritas.com")).thenReturn(Optional.of(user));

        passwordResetService.requestReset("user@veritas.com");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).deleteByUser(user);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        verify(mailService).sendEmail(eq("user@veritas.com"), anyString(), anyString());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertEquals(user, savedToken.getUser());
        assertNotNull(savedToken.getTokenHash());
        assertNotNull(savedToken.getExpiresAt());
    }

    @Test
    void RequestReset_UnknownEmail_DoesNotSendEmailOrSave() {
        when(userRepository.findByEmail("missing@veritas.com")).thenReturn(Optional.empty());

        passwordResetService.requestReset("missing@veritas.com");

        verify(mailService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("requestReset ignores blank emails")
    void RequestReset_BlankEmail_DoesNothing(String email) {
        passwordResetService.requestReset(email);

        verify(userRepository, never()).findByEmail(anyString());
        verify(passwordResetTokenRepository, never()).deleteByExpiresAtBefore(any(Instant.class));
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(mailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void RequestReset_NullEmail_DoesNothing() {
        passwordResetService.requestReset(null);

        verify(userRepository, never()).findByEmail(anyString());
        verify(passwordResetTokenRepository, never()).deleteByExpiresAtBefore(any(Instant.class));
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(mailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void RequestReset_WithQueryParameters_UsesAmpersandSeparator() {
        User user = new User();
        user.setEmail("user@veritas.com");

        when(userRepository.findByEmail("user@veritas.com")).thenReturn(Optional.of(user));
        ReflectionTestUtils.setField(passwordResetService, "resetLinkBase", "http://localhost:4200/reset-password?source=mail");

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        passwordResetService.requestReset("user@veritas.com");

        verify(mailService).sendEmail(eq("user@veritas.com"), anyString(), bodyCaptor.capture());
        String messageText = bodyCaptor.getValue();
        assertNotNull(messageText);
        assertTrue(messageText.contains("http://localhost:4200/reset-password?source=mail&token="));
    }

    @Test
    void RequestReset_MailSendFails_ThrowsDeliveryException() {
        User user = new User();
        user.setEmail("user@veritas.com");

        when(userRepository.findByEmail("user@veritas.com")).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("SMTP unavailable")).when(mailService)
            .sendEmail(anyString(), anyString(), anyString());

        assertThrows(RuntimeException.class,
            () -> passwordResetService.requestReset("user@veritas.com"));

        verify(passwordResetTokenRepository).deleteByUser(user);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void ConfirmReset_ValidToken_UpdatesPasswordAndMarksUsed() {
        User user = new User();
        user.setEmail("user@veritas.com");
        user.setRequiresPasswordChange(true);

        String token = "token-value";
        String tokenHash = hashToken(token);
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .id(1L)
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(Instant.now().plus(2, ChronoUnit.HOURS))
            .build();

        when(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
            .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPass123"))
            .thenReturn("hashed-password");

        passwordResetService.confirmReset(token, "newPass123");

        assertEquals("hashed-password", user.getPasswordHash());
        assertFalse(user.getRequiresPasswordChange());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(resetToken);
        verify(passwordResetTokenRepository).deleteByUserAndIdNot(user, 1L);
        verify(auditService).createPasswordResetLog(any(User.class), anyString(), anyString());
    }

    @Test
    void ConfirmReset_ExpiredToken_ThrowsAndDeletesToken() {
        User user = new User();
        user.setEmail("user@veritas.com");

        String token = "token-value";
        String tokenHash = hashToken(token);
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .id(2L)
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
            .build();

        when(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
            .thenReturn(Optional.of(resetToken));

        assertThrows(IllegalArgumentException.class,
            () -> passwordResetService.confirmReset(token, "newPass123"));

        verify(passwordResetTokenRepository).delete(resetToken);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void ConfirmReset_NullToken_ThrowsImmediately() {
        assertThrows(IllegalArgumentException.class,
            () -> passwordResetService.confirmReset(null, "newPass123"));

        verify(passwordResetTokenRepository, never()).findByTokenHashAndUsedAtIsNull(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(auditService, never()).createPasswordResetLog(any(User.class), anyString(), anyString());
    }

    @Test
    void ConfirmReset_BlankToken_ThrowsImmediately() {
        assertThrows(IllegalArgumentException.class,
            () -> passwordResetService.confirmReset("   ", "newPass123"));

        verify(passwordResetTokenRepository, never()).findByTokenHashAndUsedAtIsNull(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(auditService, never()).createPasswordResetLog(any(User.class), anyString(), anyString());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
