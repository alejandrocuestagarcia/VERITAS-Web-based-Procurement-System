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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

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
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        return new AuthResponseDto(accessToken, refreshToken, user.getRole().name(), user.getRequiresPasswordChange());
    }

    @Override
    @Transactional
    public void adminResetPassword(Long targetUserId, String tempPassword) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        targetUser.setPasswordHash(passwordEncoder.encode(tempPassword));
        targetUser.setRequiresPasswordChange(true);
        userRepository.save(targetUser);

        User admin = (User) org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();

        auditService.createPasswordResetLog(
                admin,
                "ADMIN_PASSWORD_RESET",
                "Reset password for user: " + targetUser.getEmail()
        );

    }

    @Override
    @Transactional
    public void completePasswordChange(String newPassword) {
        User user = (User) org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setRequiresPasswordChange(false);
        userRepository.save(user);

        auditService.createPasswordResetLog(
                user,
                "PASSWORD_CHANGED_BY_USER",
                "User successfully updated their password following an administrative reset."
        );
    }

    private String createRefreshToken(User user) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUser(user);
        rt.setExpiryDate(Instant.now().plus(1, ChronoUnit.DAYS));
        refreshTokenRepository.save(rt);
        return rt.getToken();
    }

    @Override
    public AuthResponseDto refreshToken(RefreshTokenDto refreshTokenRequest) {
        return refreshTokenRepository.findByToken(refreshTokenRequest.refreshToken())
                .map(token -> {
                    if (token.getExpiryDate().isBefore(Instant.now())) {
                        refreshTokenRepository.delete(token);
                        throw new RuntimeException("Refresh token expired.");
                    }

                    String newAccessToken = jwtService.generateAccessToken(token.getUser());

                    return new AuthResponseDto(
                            newAccessToken,
                            token.getToken(),
                            token.getUser().getRole().name(),
                            token.getUser().getRequiresPasswordChange()
                    );
                })
                .orElseThrow(() -> new RuntimeException("Refresh token not in database"));
    }

    @Override
    @Transactional
    public void logout(RefreshTokenDto refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken.refreshToken());
    }
}
