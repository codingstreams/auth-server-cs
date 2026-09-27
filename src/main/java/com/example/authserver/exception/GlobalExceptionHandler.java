package com.example.authserver.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ApiErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
    log.warn("User already exists: {}", ex.getMessage());
    ErrorCode errorCode = ex.getErrorCode() != null ? ex.getErrorCode() : ErrorCode.USER_ALREADY_EXISTS;
    ApiErrorResponse errorResponse = new ApiErrorResponse(
        errorCode.getErrorCode(),
        ex.getMessage(),
        HttpStatus.CONFLICT.value(),
        Instant.now()
    );
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
    String details = ex.getBindingResult().getFieldErrors().stream()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .reduce((a, b) -> a + "; " + b)
        .orElse("Validation failed");
    log.warn("Validation error: {}", details);
    ApiErrorResponse errorResponse = new ApiErrorResponse(
        ErrorCode.INVALID_REQUEST.getErrorCode(),
        details,
        HttpStatus.BAD_REQUEST.value(),
        Instant.now()
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleException(Exception ex) {
    log.error("Unhandled exception occurred: ", ex);
    ApiErrorResponse errorResponse = new ApiErrorResponse(
        ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(),
        "An unexpected internal server error occurred",
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        Instant.now()
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }
}
