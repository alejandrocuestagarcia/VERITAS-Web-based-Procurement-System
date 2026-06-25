package com.veritas.backend.common.exception;

/**
 * Thrown when a token refresh request fails due to an invalid, missing, or expired refresh token.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }

    public InvalidRefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
