package com.veritas.backend.auth;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {


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


  @BeforeEach
  void setUp() {
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

  private static User createTestUser(String email, UserRole role) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash("hashed_password");
    user.setRole(role);
    return user;
  }

}