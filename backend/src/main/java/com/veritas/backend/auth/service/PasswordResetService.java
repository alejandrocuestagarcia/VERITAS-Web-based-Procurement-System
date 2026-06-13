package com.veritas.backend.auth.service;

public interface PasswordResetService {

    /**
     * Initiates a password reset flow for the given email address.
     * If the email is found, any existing tokens for that user are invalidated,
     * a new signed reset link is generated and sent via email.
     * If the email is not found, the method returns silently to avoid user enumeration.
     *
     * @param email the email address of the account to reset
     */
    void requestReset(String email);

    /**
     * Completes the password reset by validating the token and updating the user's password.
     * The token is consumed on use and all other pending tokens for that user are invalidated.
     * The action is audit-logged on success.
     *
     * @param token the raw reset token from the reset link
     * @param newPassword the new plain-text password to set
     * @throws IllegalArgumentException if the token is missing, invalid, already used, or expired
     */
    void confirmReset(String token, String newPassword);
}
