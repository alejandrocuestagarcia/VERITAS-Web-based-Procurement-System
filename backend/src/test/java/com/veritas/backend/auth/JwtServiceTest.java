package com.veritas.backend.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

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
        org.springframework.security.core.userdetails.User.withUsername("dev@veritas.com")
            .password("password").authorities("ROLE_ADMINISTRATOR").build();

  }

  @Test
  void should_GenerateAndExtractSubject_Successfully() {


    String token = jwtService.generateAccessToken(adminUser);
    String extractEmail = jwtService.extractEmail(token);

    assertNotNull(token);
    assertEquals("dev@veritas.com", extractEmail);
  }

  @Test
  void extractEmail() {
  }

  @Test
  void isTokenValid_ShouldReturnTrue_WhenDetailsMatch() {

    String token = jwtService.generateAccessToken(adminUser);



    assertTrue(jwtService.isTokenValid(token, adminUserDetails));
  }

  @Test
  void isTokenValid_ShouldReturnFalse_WhenTokenExpiredOneMillisecondAgo() {

    ReflectionTestUtils.setField(jwtService, "accessTokenExpire", -1L);

    String expiredToken = jwtService.generateAccessToken(adminUser);

    assertFalse(jwtService.isTokenValid(expiredToken, adminUserDetails));

  }

  @Test
  void isTokenValid_ShouldReturnFalse_WhenTokenHasZeroMillisecondsExpiryDuration() {

    ReflectionTestUtils.setField(jwtService, "accessTokenExpire", 0L);

    String expiredToken = jwtService.generateAccessToken(adminUser);

    assertFalse(jwtService.isTokenValid(expiredToken, adminUserDetails));

  }

  @Test
  void extractClaim_shouldExtractSubject_Successfully() {
    String token = jwtService.generateAccessToken(adminUser);

    String subject = jwtService.extractClaim(token, Claims::getSubject);

    assertEquals("dev@veritas.com", subject);
  }

  @Test
  void extractClaim_shouldExtractCustomRoleClaim_Successfully() {
    String token = jwtService.generateAccessToken(adminUser);

    String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));

    assertEquals("ADMINISTRATOR", role);
  }

  @Test
  void extractClaim_shouldExtractExpirationDate() {

    String token = jwtService.generateAccessToken(adminUser);

    Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

    assertNotNull(expiration);
    assertTrue(expiration.after(new Date()));
  }
}