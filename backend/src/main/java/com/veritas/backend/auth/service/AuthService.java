package com.veritas.backend.auth.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import com.veritas.backend.auth.dto.AuthResponseDto;
import com.veritas.backend.auth.dto.LoginRequestDto;
import com.veritas.backend.auth.dto.RefreshTokenDto;

public interface AuthService {

  /**
     * Authenticates a user by email and password, returning a new access and refresh token pair.
     *
     * @param request the login credentials
     * @return an {@link AuthResponseDto} containing the access token, refresh token, role, and password change flag
     * @throws BadCredentialsException if the email or password is invalid
     * @throws DisabledException if the user account is disabled
  */
  AuthResponseDto login(LoginRequestDto request);

  /**
     * Issues a new access token for a valid, non-expired refresh token.
     * The refresh token itself is not rotated.
     *
     * @param refreshTokenRequest the DTO containing the refresh token string
     * @return an {@link AuthResponseDto} with a new access token and the same refresh token
     * @throws RuntimeException if the token is not found or has expired
  */
  AuthResponseDto refreshToken(RefreshTokenDto refreshTokenRequest);

  /**
     * Invalidates the given refresh token, effectively logging the user out.
     *
     * @param refreshToken the DTO containing the refresh token to delete
  */
  void logout(RefreshTokenDto refreshToken);

  /**
     * Resets a user's password to a temporary value and flags their account to require a password change on next login.
     * The acting admin is resolved from the current security context and the action is audit-logged.
     *
     * @param targetUserId the ID of the user whose password is being reset
     * @param tempPassword the temporary plain-text password to set
     * @throws RuntimeException if no user exists with the given ID
  */
  void adminResetPassword(Long targetUserId, String tempPassword);

  /**
     * Completes a forced password change for the currently authenticated user,
     * updating their password and clearing the password change requirement flag.
     * The action is audit-logged.
     *
     * @param newPassword the new plain-text password to set
  */
  void completePasswordChange(String newPassword);
}
