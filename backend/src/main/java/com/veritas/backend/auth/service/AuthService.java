package com.veritas.backend.auth.service;

import com.veritas.backend.auth.dto.AuthResponseDto;
import com.veritas.backend.auth.dto.LoginRequestDto;
import com.veritas.backend.auth.dto.RefreshTokenDto;

public interface AuthService {
  AuthResponseDto login(LoginRequestDto request);

  AuthResponseDto refreshToken(RefreshTokenDto refreshTokenRequest);
}
