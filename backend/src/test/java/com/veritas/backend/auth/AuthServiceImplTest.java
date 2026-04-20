package com.veritas.backend.auth;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import com.veritas.backend.auth.dto.AuthResponseDto;
import com.veritas.backend.auth.dto.LoginRequestDto;
import com.veritas.backend.auth.dto.RefreshTokenDto;
import com.veritas.backend.auth.entity.RefreshToken;
import com.veritas.backend.auth.repository.RefreshTokenRepository;
import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.auth.service.impl.AuthServiceImpl;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  private static final String CORRECT_EMAIL = "test@veritas.com", FALSE_EMAIL = "unknown@veritas.com",
          CORRECT_PASSWORD = "password123", HASHED_PASSWORD = "hashedpassword", FALSE_PASSWORD = "wrongpassword";


  @Mock
  private UserRepository userRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private RefreshTokenRepository refreshTokenRepository;
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
  void login_shouldReturnAuthResponse_WhenCredentialsAreValid() {

    User user = createTestUser("dev@veritas.com", UserRole.ADMINISTRATOR);

    LoginRequestDto requestDto = new LoginRequestDto("dev@veritas.com", "password");

    when(userRepository.findByEmail("dev@veritas.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password", "hashed_password")).thenReturn(true);
    when(jwtService.generateAccessToken(user)).thenReturn("mock_access_token");


    AuthResponseDto authResponseDto = authService.login(requestDto);

    assertThat(authResponseDto).isNotNull();

    assertThat(authResponseDto.accessToken()).isEqualTo("mock_access_token");
    assertThat(authResponseDto.role()).isEqualTo(UserRole.ADMINISTRATOR.name());


    verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));

  }

  @Test
  void login_shouldThrowBadCredential_whenEmailIsInvalid() {

    createTestUser("dev@veritas.com", UserRole.ADMINISTRATOR);

    LoginRequestDto requestDto = new LoginRequestDto("invalid@veritas.com", "password");

    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

    assertThrows(BadCredentialsException.class, () -> authService.login(requestDto));


  }

  @Test
  void login_shouldThrowBadCredential_whenPasswordIsInvalid() {

    User user = createTestUser("dev@veritas.com", UserRole.ADMINISTRATOR);

    LoginRequestDto requestDto = new LoginRequestDto("dev@veritas.com", "invalid");
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

    assertThrows(BadCredentialsException.class, () -> authService.login(requestDto));


  }

  @Test
  void refreshToken_shouldReturnNewAccessToken_WhenRefreshTokenIsValid() {

    User user = createTestUser("dev@veritas.com", UserRole.ADMINISTRATOR);

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken("valid_token");
    refreshToken.setUser(user);
    refreshToken.setExpiryDate(Instant.now().plusSeconds(60));

    when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(refreshToken));
    when(jwtService.generateAccessToken(user)).thenReturn("new_access_token");

    AuthResponseDto response = authService.refreshToken(new RefreshTokenDto("valid_token"));

    assertThat(response.accessToken()).isEqualTo("new_access_token");

  }

  @Test
  void refreshToken_shouldThrowRuntimeException_WhenRefreshTokenNotFound() {

    User user = createTestUser("dev@veritas.com", UserRole.ADMINISTRATOR);

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken("valid_token");
    refreshToken.setUser(user);
    refreshToken.setExpiryDate(Instant.now().plusSeconds(60));

    when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());


    assertThrows(RuntimeException.class, () -> authService.refreshToken(new RefreshTokenDto("invalid_token")));

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

    Assertions.assertThrows(BadCredentialsException.class, () -> authService.login(request));

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

    Assertions.assertThrows(BadCredentialsException.class, () -> authService.login(request));

    verify(userRepository).findByEmail(CORRECT_EMAIL);
    verify(passwordEncoder).matches(FALSE_PASSWORD, HASHED_PASSWORD);
    verify(jwtService, never()).generateAccessToken(any(User.class));
    verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
  }

  private static User createTestUser(String email, UserRole role) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash("hashed_password");
    user.setRole(role);
    return user;
  }

}