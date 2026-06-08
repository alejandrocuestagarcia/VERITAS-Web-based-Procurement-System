package com.veritas.backend.auth.service.impl;

import com.veritas.backend.audit.service.AuditService;
import com.veritas.backend.auth.entity.PasswordResetToken;
import com.veritas.backend.auth.repository.PasswordResetTokenRepository;
import com.veritas.backend.auth.service.PasswordResetService;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.repository.UserRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.veritas.backend.mail.MailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.veritas.backend.common.model.AuditActionConstants.PASSWORD_RESET_EMAIL;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AuditService auditService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.password-reset.expiration-hours:24}")
    private long expirationHours;

    @Value("${security.password-reset.reset-link-base}")
    private String resetLinkBase;



    @Value("${security.password-reset.subject:Reset your Veritas password}")
    private String subject;

    @Override
    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String trimmedEmail = email.trim();
        passwordResetTokenRepository.deleteByExpiresAtBefore(Instant.now());

        userRepository.findByEmail(trimmedEmail).ifPresentOrElse(user -> {
            passwordResetTokenRepository.deleteByUser(user);

            String token = generateToken();
            String tokenHash = hashToken(token);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(expirationHours, ChronoUnit.HOURS))
                .build();

            passwordResetTokenRepository.save(resetToken);

            String resetLink = buildResetLink(token);
            sendResetEmail(user, resetLink);
        }, () -> log.info("Password reset requested for unknown email"));
    }

    @Override
    @Transactional
    public void confirmReset(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Invalid or expired reset token.");
        }

        String tokenHash = hashToken(token.trim());
        PasswordResetToken resetToken = passwordResetTokenRepository
            .findByTokenHashAndUsedAtIsNull(tokenHash)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token."));

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Reset token expired.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setRequiresPasswordChange(false);
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);
        passwordResetTokenRepository.deleteByUserAndIdNot(user, resetToken.getId());

        auditService.createPasswordResetLog(
            user,
            PASSWORD_RESET_EMAIL,
            "User " + user.getEmail() + " reset their password via email."
        );
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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

    private String buildResetLink(String token) {
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String separator = resetLinkBase.contains("?") ? "&" : "?";
        return resetLinkBase + separator + "token=" + encodedToken;
    }

    private void sendResetEmail(User user, String resetLink) {
        String body = "We received a request to reset your password.\n\n"
            + "Reset link (valid for " + expirationHours + " hours):\n"
            + resetLink + "\n\n"
            + "If you did not request this, you can ignore this email.";
        mailService.sendEmail(user.getEmail(), subject, body);
    }
}
