package com.veritas.backend.common.exception;

import com.veritas.backend.workflow.validation.BpmnValidationException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GeneralExceptionHandler {

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<String> handleBadCredentialsException(final BadCredentialsException exception) {
    log.warn("Bad credentials: {}", exception.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
  }

  @ExceptionHandler(DisabledException.class)
  public ResponseEntity<String> handleDisabledException(final DisabledException exception) {
    log.warn("Account disabled: {}", exception.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Account is disabled");
  }

  @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
  public ResponseEntity<String> handleAccessDenied(RuntimeException exception) {
    log.warn("Access denied: {}", exception.getMessage());
    String message = exception.getMessage();
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message != null ? message : "Access Denied");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.put(error.getField(), error.getDefaultMessage());
    }

    log.warn("Validation failed: {}", errors);
    return ResponseEntity.badRequest().body(errors);
  }
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgumentError(IllegalArgumentException exception) {
    log.warn("Illegal argument: {}", exception.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<String> handleNotFound(EntityNotFoundException ex) {
    log.warn("Entity not found: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<String> handleIllegalStateException(IllegalStateException ex) {
    log.warn("Illegal state: " + ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
  }

  @ExceptionHandler(EntityExistsException.class)
  public ResponseEntity<String> handleConflict(EntityExistsException ex) {
    log.warn("Entity conflict: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    log.error("Data integrity violation: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("An unexpected database error occurred. Please contact support.");
  }

  @ExceptionHandler(MailException.class)
  public ResponseEntity<String> handleMailException(MailException ex) {
    log.error("Mail delivery failed: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("Unable to send password reset email. Please try again later.");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleGeneralError(Exception exception) {
    log.error("Unhandled exception: {}", exception.getMessage(), exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected internal server error occurred. Please contact support.");
  }

  @ExceptionHandler(WorkflowStateException.class)
  public ResponseEntity<String> handleWorkflowState(final WorkflowStateException exception) {
    log.warn("Workflow state violation: {}", exception.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
  }

  @ExceptionHandler(BpmnValidationException.class)
  public ResponseEntity<Object> handleBpmnValidation(final BpmnValidationException exception) {
    log.warn("BPMN validation failed with {} error(s): {}", exception.getErrors().size(), exception.getMessage());
    Map<String, Object> body = new HashMap<>();
    body.put("errors", exception.getErrors());
    body.put("warnings", exception.getWarnings());
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<String> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
    log.warn("Max upload size exceeded: {}", exception.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Maximum upload size exceeded. Please keep files under 10MB.");
  }
}

