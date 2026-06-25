package com.veritas.backend.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import com.veritas.backend.audit.service.AuditService;
import com.veritas.backend.auth.dto.AuthResponseDto;
import com.veritas.backend.auth.dto.LoginRequestDto;
import com.veritas.backend.auth.dto.RefreshTokenDto;
import com.veritas.backend.common.exception.InvalidRefreshTokenException;
import com.veritas.backend.auth.entity.RefreshToken;
import com.veritas.backend.auth.repository.RefreshTokenRepository;
import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.auth.service.impl.AuthServiceImpl;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.springframework.security.core.Authentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

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

  @Mock
  private AuditService auditService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail(CORRECT_EMAIL);
    testUser.setIsActive(true);
    testUser.setPasswordHash(HASHED_PASSWORD);
    testUser.setRole(UserRole.REQUESTER);
  }

  @AfterEach
  void tearDown() {
      SecurityContextHolder.clearContext();
  }

  @Test
  void Login_ValidCredentials_ReturnsAuthResponse() {

    User user = createTestUser("dev@veritas.com", UserRole.ADMINISTRATOR);

    LoginRequestDto requestDto = new LoginRequestDto("dev@veritas.com", "password");

    when(userRepository.findByEmail("dev@veritas.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password", "hashed_password")).thenReturn(true);
    when(jwtService.generateAccessToken(user)).thenReturn("mock_access_token");


    AuthResponseDto authResponseDto = authService.login(requestDto);

    assertAll(
        () -> assertNotNull(authResponseDto),
        () -> assertEquals("mock_access_token", authResponseDto.accessToken()),
        () -> assertEquals(UserRole.ADMINISTRATOR.name(), authResponseDto.role())
    );

    verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));

  }

  @Test
  void Login_InvalidEmail_ThrowsBadCredentialsException() {

    createTestUser("dev@veritas.com", UserRole.ADMINISTRATOR);

    LoginRequestDto requestDto = new LoginRequestDto("invalid@veritas.com", "password");

    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

    assertThrows(BadCredentialsException.class, () -> authService.login(requestDto));

  }

  @Test
  void Login_InvalidPassword_ThrowsBadCredentialsException() {

    User user = createTestUser("dev@veritas.com", UserRole.ADMINISTRATOR);

    LoginRequestDto requestDto = new LoginRequestDto("dev@veritas.com", "invalid");
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

    assertThrows(BadCredentialsException.class, () -> authService.login(requestDto));

  }

  @Test
  void RefreshToken_ValidToken_ReturnsNewAccessToken() {

    User user = createTestUser("dev@veritas.com", UserRole.ADMINISTRATOR);

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken("valid_token");
    refreshToken.setUser(user);
    refreshToken.setExpiryDate(Instant.now().plusSeconds(60));

    when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(refreshToken));
    when(jwtService.generateAccessToken(user)).thenReturn("new_access_token");

    AuthResponseDto response = authService.refreshToken(new RefreshTokenDto("valid_token"));

    assertEquals("new_access_token", response.accessToken());

  }

  @Test
  void RefreshToken_TokenNotFound_ThrowsInvalidRefreshTokenException() {

    User user = createTestUser("dev@veritas.com", UserRole.ADMINISTRATOR);

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken("valid_token");
    refreshToken.setUser(user);
    refreshToken.setExpiryDate(Instant.now().plusSeconds(60));

    when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());


    assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken(new RefreshTokenDto("invalid_token")));

  }

  @Test
  void Login_ValidRequest_ReturnsAuthResponseAndSavesToken() {
    LoginRequestDto request = new LoginRequestDto(CORRECT_EMAIL, CORRECT_PASSWORD);

    when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches(CORRECT_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
    when(jwtService.generateAccessToken(testUser)).thenReturn("mocked-jwt-token");
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

    AuthResponseDto response = authService.login(request);

    assertAll(
        () -> assertNotNull(response),
        () -> assertEquals("mocked-jwt-token", response.accessToken()),
        () -> assertNotNull(response.refreshToken()),
        () -> assertEquals(UserRole.REQUESTER.name(), response.role())
    );

    verify(userRepository).findByEmail(CORRECT_EMAIL);
    verify(passwordEncoder).matches(CORRECT_PASSWORD, HASHED_PASSWORD);
    verify(jwtService).generateAccessToken(testUser);
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  void Login_UserNotFound_ThrowsBadCredentialsException() {
    LoginRequestDto request = new LoginRequestDto(FALSE_EMAIL, CORRECT_PASSWORD);
    when(userRepository.findByEmail(FALSE_EMAIL)).thenReturn(Optional.empty());

    assertThrows(BadCredentialsException.class, () -> authService.login(request));

    verify(userRepository).findByEmail(FALSE_EMAIL);
    verify(passwordEncoder, never()).matches(anyString(), anyString());
    verify(jwtService, never()).generateAccessToken(any(User.class));
    verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
  }

  @Test
  void Login_WrongPassword_ThrowsBadCredentialsException() {
    LoginRequestDto request = new LoginRequestDto(CORRECT_EMAIL, FALSE_PASSWORD);

    when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches(FALSE_PASSWORD, HASHED_PASSWORD)).thenReturn(false);

    assertThrows(BadCredentialsException.class, () -> authService.login(request));

    verify(userRepository).findByEmail(CORRECT_EMAIL);
    verify(passwordEncoder).matches(FALSE_PASSWORD, HASHED_PASSWORD);
    verify(jwtService, never()).generateAccessToken(any(User.class));
    verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
  }

  private static User createTestUser(String email, UserRole role) {
    User user = new User();
    user.setEmail(email);
    user.setIsActive(true);
    user.setPasswordHash("hashed_password");
    user.setRole(role);
    return user;
  }

  @Test
  void AdminResetPassword_ValidRequest_FlagsUserAndCallsAudit() {
    Long userId = 10L;
    User target = User.builder().id(userId).email("target@v.com").build();
    User admin = User.builder().id(1L).email("admin@v.com").build();

    Authentication auth = mock(Authentication.class);
    SecurityContext securityContext = mock(SecurityContext.class);

    when(securityContext.getAuthentication()).thenReturn(auth);
    when(auth.getPrincipal()).thenReturn(admin);
    SecurityContextHolder.setContext(securityContext);

    when(userRepository.findById(userId)).thenReturn(Optional.of(target));
    when(passwordEncoder.encode(anyString())).thenReturn("newHashedPass");

    authService.adminResetPassword(userId, "newTempPass");

    assertTrue(target.getRequiresPasswordChange());
    verify(auditService).createPasswordResetLog(eq(admin), eq("ADMIN_PASSWORD_RESET"), anyString());
  }

  @Test
  void Login_UserDisabled_ThrowsDisabledException() {
    LoginRequestDto request = new LoginRequestDto(CORRECT_EMAIL, CORRECT_PASSWORD);
    testUser.setIsActive(false);

    when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches(CORRECT_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

    assertThrows(DisabledException.class, () -> authService.login(request));

    verify(userRepository).findByEmail(CORRECT_EMAIL);
    verify(passwordEncoder).matches(CORRECT_PASSWORD, HASHED_PASSWORD);
    verify(jwtService, never()).generateAccessToken(any(User.class));
    verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
  }

  @Test
  void RefreshToken_TokenExpired_ThrowsInvalidRefreshTokenExceptionAndDeletesToken() {
    User user = createTestUser("dev@veritas.com", UserRole.ADMINISTRATOR);
    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken("expired_token");
    refreshToken.setUser(user);
    refreshToken.setExpiryDate(Instant.now().minusSeconds(10));

    when(refreshTokenRepository.findByToken("expired_token")).thenReturn(Optional.of(refreshToken));

    assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken(new RefreshTokenDto("expired_token")));

    verify(refreshTokenRepository).findByToken("expired_token");
    verify(refreshTokenRepository).delete(refreshToken);
    verify(jwtService, never()).generateAccessToken(any());
  }

  @Test
  void CompletePasswordChange_ValidRequest_UpdatesPasswordAndAuditLog() {
    User admin = User.builder().id(1L).email("admin@v.com").build();
    Authentication auth = mock(Authentication.class);
    SecurityContext securityContext = mock(SecurityContext.class);

    when(securityContext.getAuthentication()).thenReturn(auth);
    when(auth.getPrincipal()).thenReturn(admin);
    SecurityContextHolder.setContext(securityContext);

    when(passwordEncoder.encode("newPassword")).thenReturn("newHashedPassword");

    authService.completePasswordChange("newPassword");

    assertAll(
        () -> assertEquals("newHashedPassword", admin.getPasswordHash()),
        () -> assertFalse(admin.getRequiresPasswordChange())
    );
    verify(userRepository).save(admin);
    verify(auditService).createPasswordResetLog(eq(admin), eq("PASSWORD_CHANGED_BY_USER"), anyString());
  }

  @Test
  void Logout_ValidToken_DeletesRefreshToken() {
    authService.logout(new RefreshTokenDto("token_to_delete"));
    verify(refreshTokenRepository).deleteByToken("token_to_delete");
  }

}