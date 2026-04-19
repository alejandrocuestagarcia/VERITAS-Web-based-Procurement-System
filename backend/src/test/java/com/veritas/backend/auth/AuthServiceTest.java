package com.veritas.backend.auth;

import com.veritas.backend.auth.dto.AuthResponseDto;
import com.veritas.backend.auth.dto.LoginRequestDto;
import com.veritas.backend.auth.entity.RefreshToken;
import com.veritas.backend.auth.repository.RefreshTokenRepository;
import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.auth.service.impl.AuthServiceImpl;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final String CORRECT_EMAIL = "test@veritas.com", FALSE_EMAIL = "unknown@veritas.com",
            CORRECT_PASSWORD = "password123", HASHED_PASSWORD = "hashedpassword", FALSE_PASSWORD = "wrongpassword";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(CORRECT_EMAIL);
        testUser.setPasswordHash(HASHED_PASSWORD);
        testUser.setRole(UserRole.REQUESTER);
    }

    @Test
    void testLoginSuccess() {
        LoginRequestDto request = new LoginRequestDto(CORRECT_EMAIL, CORRECT_PASSWORD);
        
        when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CORRECT_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("mocked-jwt-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        AuthResponseDto response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals(UserRole.REQUESTER.name(), response.role());

        verify(userRepository).findByEmail(CORRECT_EMAIL);
        verify(passwordEncoder).matches(CORRECT_PASSWORD, HASHED_PASSWORD);
        verify(jwtService).generateAccessToken(testUser);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void testLoginUserNotFound() {
        LoginRequestDto request = new LoginRequestDto(FALSE_EMAIL, CORRECT_PASSWORD);
        when(userRepository.findByEmail(FALSE_EMAIL)).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        
        verify(userRepository).findByEmail(FALSE_EMAIL);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateAccessToken(any(User.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void testLoginInvalidPassword() {
        LoginRequestDto request = new LoginRequestDto(CORRECT_EMAIL, FALSE_PASSWORD);
        
        when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(FALSE_PASSWORD, HASHED_PASSWORD)).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        verify(userRepository).findByEmail(CORRECT_EMAIL);
        verify(passwordEncoder).matches(FALSE_PASSWORD, HASHED_PASSWORD);
        verify(jwtService, never()).generateAccessToken(any(User.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }
}
