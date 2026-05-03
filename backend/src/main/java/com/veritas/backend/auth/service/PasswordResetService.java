package com.veritas.backend.auth.service;

public interface PasswordResetService {
    void requestReset(String email);

    void confirmReset(String token, String newPassword);
}
