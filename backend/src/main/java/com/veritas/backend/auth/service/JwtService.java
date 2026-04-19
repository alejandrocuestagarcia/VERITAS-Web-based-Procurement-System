package com.veritas.backend.auth.service;

import com.veritas.backend.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  @Value("${security.jwt.secret-key}")
  private String secretKey;

  @Value("${security.jwt.access-token-expiration}")
  private long accessTokenExpire;

  public String generateAccessToken(User user) {
    return Jwts.builder().subject(user.getEmail()).claim("role", user.getRole().name())
        .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + accessTokenExpire))
        .signWith(getSigninKey()).compact();
  }

  public String extractEmail(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {

    try {


      final String email = extractEmail(token);
      return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    } catch (ExpiredJwtException e) {
      //Maybe handle this exception in a separate global exceptionhandler
      return false;
    }
  }

  private boolean isTokenExpired(String token) {
    return extractClaim(token, Claims::getExpiration).before(new Date());
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSigninKey()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey getSigninKey() {
    byte[] keyBytes = Decoders.BASE64URL.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
