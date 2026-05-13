package com.example.desofs.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for REST API
 * To be implemented with @RestControllerAdvice for centralized error handling
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String correlationId = UUID.randomUUID().toString();
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        logger.warn("Validation error [{}]: {}", correlationId, message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(correlationId, HttpStatus.BAD_REQUEST.value(), message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Bad request [{}]: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(correlationId, HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Conflict [{}]: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(correlationId, HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Access denied [{}]: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody(correlationId, HttpStatus.FORBIDDEN.value(), "Access denied"));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.error("Security violation [{}]: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(correlationId, HttpStatus.BAD_REQUEST.value(), "Invalid request"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.error("Unexpected error [{}]: {}", correlationId, ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(correlationId, HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred"));
    }

    private Map<String, Object> errorBody(String correlationId, int status, String message) {
        return Map.of(
                "correlationId", correlationId,
                "status", status,
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
