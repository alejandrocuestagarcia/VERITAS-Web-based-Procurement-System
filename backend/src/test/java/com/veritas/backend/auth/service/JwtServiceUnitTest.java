package com.veritas.backend.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.core.userdetails.User.withUsername;

import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceUnitTest {

  private JwtService jwtService;

  private User adminUser;

  private UserDetails adminUserDetails;

  @BeforeEach
  void setUp() throws Exception {
    jwtService = new JwtService();
    ReflectionTestUtils.setField(jwtService, "secretKey", "testSecretKeyWhichIsusedofrtestingonlyandwhichisextremlysecure==");
    ReflectionTestUtils.setField(jwtService, "accessTokenExpire", 3600000L);

    adminUser = new User();
    adminUser.setEmail("dev@veritas.com");
    adminUser.setRole(UserRole.ADMINISTRATOR);

    adminUserDetails =
        withUsername("dev@veritas.com")
            .password("password").authorities("ROLE_ADMINISTRATOR").build();

  }

  @Test
  void GenerateAndExtractSubject_ValidUser_ReturnsSubject() {


    String token = jwtService.generateAccessToken(adminUser);
    String extractEmail = jwtService.extractEmail(token);

    assertNotNull(token);
    assertEquals("dev@veritas.com", extractEmail);
  }

  @Test
  void ExtractEmail_ValidToken_ReturnsEmail() {
    String token = jwtService.generateAccessToken(adminUser);
    assertEquals("dev@veritas.com", jwtService.extractEmail(token));
  }

  @Test
  void IsTokenValid_DetailsMatch_ReturnsTrue() {
    String token = jwtService.generateAccessToken(adminUser);
    assertTrue(jwtService.isTokenValid(token, adminUserDetails));
  }

  @Test
  void IsTokenValid_UsernameMismatched_ReturnsFalse() {
    String token = jwtService.generateAccessToken(adminUser);
    UserDetails differentUser = withUsername("wrong@veritas.com")
        .password("password").authorities("ROLE_ADMINISTRATOR").build();
    assertFalse(jwtService.isTokenValid(token, differentUser));
  }

  @Test
  void IsTokenValid_TokenExpired_ReturnsFalse() {

    ReflectionTestUtils.setField(jwtService, "accessTokenExpire", -1L);

    String expiredToken = jwtService.generateAccessToken(adminUser);

    assertFalse(jwtService.isTokenValid(expiredToken, adminUserDetails));

  }

  @Test
  void IsTokenValid_ZeroExpiry_ReturnsFalse() {

    ReflectionTestUtils.setField(jwtService, "accessTokenExpire", 0L);

    String expiredToken = jwtService.generateAccessToken(adminUser);

    assertFalse(jwtService.isTokenValid(expiredToken, adminUserDetails));

  }

  @Test
  void ExtractClaim_SubjectClaim_ReturnsSubject() {
    String token = jwtService.generateAccessToken(adminUser);

    String subject = jwtService.extractClaim(token, Claims::getSubject);

    assertEquals("dev@veritas.com", subject);
  }

  @Test
  void ExtractClaim_CustomRoleClaim_ReturnsRole() {
    String token = jwtService.generateAccessToken(adminUser);

    String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));

    assertEquals("ADMINISTRATOR", role);
  }

  @Test
  void ExtractClaim_ExpirationClaim_ReturnsExpirationDate() {

    String token = jwtService.generateAccessToken(adminUser);

    Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

    assertNotNull(expiration);
    assertTrue(expiration.after(new Date()));
  }
}