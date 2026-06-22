package com.veritas.backend.common.exception;

import com.veritas.backend.workflow.validation.BpmnValidationException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
  public ResponseEntity<Map<String, Object>> handleBadCredentialsException(final BadCredentialsException exception) {
    log.warn("Bad credentials: {}", exception.getMessage());
    return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password");
  }

  @ExceptionHandler(DisabledException.class)
  public ResponseEntity<Map<String, Object>> handleDisabledException(final DisabledException exception) {
    log.warn("Account disabled: {}", exception.getMessage());
    return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Account is disabled");
  }

  @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
  public ResponseEntity<Map<String, Object>> handleAccessDenied(RuntimeException exception) {
    log.warn("Access denied: {}", exception.getMessage());
    String message = exception.getMessage();
    return buildErrorResponse(HttpStatus.FORBIDDEN, message != null ? message : "Access Denied");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
    List<String> messages = new ArrayList<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      messages.add(error.getField() + ": " + error.getDefaultMessage());
    }
    String message = String.join(", ", messages);
    log.warn("Validation failed: {}", message);
    return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
  }
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgumentError(IllegalArgumentException exception) {
    log.warn("Illegal argument: {}", exception.getMessage());
    return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
    log.warn("Entity not found: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
    log.warn("Illegal state: " + ex.getMessage());
    return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(EntityExistsException.class)
  public ResponseEntity<Map<String, Object>> handleConflict(EntityExistsException ex) {
    log.warn("Entity conflict: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    log.error("Data integrity violation: {}", ex.getMessage(), ex);
    return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected database error occurred. Please contact support.");
  }

  @ExceptionHandler(MailException.class)
  public ResponseEntity<Map<String, Object>> handleMailException(MailException ex) {
    log.error("Mail delivery failed: {}", ex.getMessage(), ex);
    return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
        "Unable to send password reset email. Please try again later.");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneralError(Exception exception) {
    log.error("Unhandled exception: {}", exception.getMessage(), exception);
    return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected internal server error occurred. Please contact support.");
  }

  @ExceptionHandler(WorkflowStateException.class)
  public ResponseEntity<Map<String, Object>> handleWorkflowState(final WorkflowStateException exception) {
    log.warn("Workflow state violation: {}", exception.getMessage());
    return buildErrorResponse(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(BpmnValidationException.class)
  public ResponseEntity<Map<String, Object>> handleBpmnValidation(final BpmnValidationException exception) {
    log.warn("BPMN validation failed with {} error(s): {}", exception.getErrors().size(), exception.getMessage());
    String message = "Errors:\n- " + String.join("\n- ", exception.getErrors());
    if (!exception.getWarnings().isEmpty()) {
      message += "\n\nWarnings:\n" + String.join("\n- ", exception.getWarnings());
    }
    return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, message);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
    log.warn("Max upload size exceeded: {}", exception.getMessage());
    return buildErrorResponse(HttpStatus.BAD_REQUEST,
        "Maximum upload size exceeded. Please keep files under 10MB.");
  }

  private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
    Map<String, Object> body = new HashMap<>();
    body.put("message", message);
    return ResponseEntity.status(status).body(body);
  }
}

