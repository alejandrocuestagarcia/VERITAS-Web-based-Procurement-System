package com.veritas.backend.auth.service.impl;

import com.veritas.backend.audit.service.AuditService;
import com.veritas.backend.auth.dto.AuthResponseDto;
import com.veritas.backend.auth.dto.LoginRequestDto;
import com.veritas.backend.auth.dto.RefreshTokenDto;
import com.veritas.backend.auth.entity.RefreshToken;
import com.veritas.backend.auth.repository.RefreshTokenRepository;
import com.veritas.backend.auth.service.AuthService;
import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.veritas.backend.common.model.AuditActionConstants.ADMIN_PASSWORD_RESET;
import static com.veritas.backend.common.model.AuditActionConstants.PASSWORD_CHANGED_BY_USER;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;


    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        log.info("Login attempt for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed – email not found: {}", request.email());
                    return new BadCredentialsException("Invalid credentials");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed – invalid password for email: {}", request.email());
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        log.info("Login successful for user: {} (role={})", user.getEmail(), user.getRole());
        return new AuthResponseDto(accessToken, refreshToken, user.getRole().name(), user.getRequiresPasswordChange());
    }

    @Override
    @Transactional
    public void adminResetPassword(Long targetUserId, String tempPassword) {
        log.info("Admin password reset requested for userId={}", targetUserId);

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        targetUser.setPasswordHash(passwordEncoder.encode(tempPassword));
        targetUser.setRequiresPasswordChange(true);
        userRepository.save(targetUser);

        User admin = (User) org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();

        log.info("Admin password reset completed by admin={} for targetUser={}",
            admin.getEmail(), targetUser.getEmail());

        auditService.createPasswordResetLog(
                admin,
                ADMIN_PASSWORD_RESET,
                "Reset password for user: " + targetUser.getEmail()
        );

    }

    @Override
    @Transactional
    public void completePasswordChange(String newPassword) {
        User user = (User) org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();

        log.info("User initiated password change for user={}", user.getEmail());

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setRequiresPasswordChange(false);
        userRepository.save(user);

        log.info("Password change completed for user={}", user.getEmail());

        auditService.createPasswordResetLog(
                user,
                PASSWORD_CHANGED_BY_USER,
                "User " + user.getEmail() + " successfully updated their password following an administrative reset."
        );
    }

    private String createRefreshToken(User user) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUser(user);
        rt.setExpiryDate(Instant.now().plus(1, ChronoUnit.DAYS));
        refreshTokenRepository.save(rt);
        log.debug("Created refresh token for user: {}", user.getEmail());
        return rt.getToken();
    }

    @Override
    public AuthResponseDto refreshToken(RefreshTokenDto refreshTokenRequest) {
        log.debug("Token refresh requested");

        return refreshTokenRepository.findByToken(refreshTokenRequest.refreshToken())
                .map(token -> {
                    if (token.getExpiryDate().isBefore(Instant.now())) {
                        refreshTokenRepository.delete(token);
                        log.warn("Refresh token expired for user: {}", token.getUser().getEmail());
                        throw new RuntimeException("Refresh token expired.");
                    }

                    String newAccessToken = jwtService.generateAccessToken(token.getUser());

                    log.info("Token refreshed successfully for user: {}", token.getUser().getEmail());
                    return new AuthResponseDto(
                            newAccessToken,
                            token.getToken(),
                            token.getUser().getRole().name(),
                            token.getUser().getRequiresPasswordChange()
                    );
                })
                .orElseThrow(() -> {
                    log.warn("Refresh token not found in database");
                    return new RuntimeException("Refresh token not in database");
                });
    }

    @Override
    @Transactional
    public void logout(RefreshTokenDto refreshToken) {
        log.info("Logout requested – invalidating refresh token");
        refreshTokenRepository.deleteByToken(refreshToken.refreshToken());
        log.debug("Refresh token deleted successfully");
    }
}
