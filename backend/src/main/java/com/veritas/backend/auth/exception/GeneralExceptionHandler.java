package com.veritas.backend.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GeneralExceptionHandler {

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<String> handleBadCredentialsException(final BadCredentialsException exception) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
  }

  @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
  public ResponseEntity<String> handleAccessDenied(Exception exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleGeneralError(Exception exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
  }
}
