package com.example.desofs.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for the REST API.
 *
 * <p>Centralizes validation, authorization, business-rule, security, and
 * unexpected error responses into a consistent JSON structure.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles bean validation failures raised while binding request payloads.
     *
     * @param ex validation exception describing the invalid fields
     * @return HTTP 400 response with a correlation id and validation message
     */
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

    /**
     * Handles invalid request data and application-level bad input.
     *
     * @param ex exception describing the invalid request
     * @return HTTP 400 response with a correlation id and error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Bad request [{}]: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(correlationId, HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    /**
     * Handles business-rule conflicts such as invalid state transitions.
     *
     * @param ex exception describing the conflict
     * @return HTTP 409 response with a correlation id and conflict message
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Conflict [{}]: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(correlationId, HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    /**
     * Handles authorization failures detected by the security layer.
     *
     * @param ex access-denied exception
     * @return HTTP 403 response with a sanitized error body
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Access denied [{}]: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody(correlationId, HttpStatus.FORBIDDEN.value(), "Access denied"));
    }

    /**
     * Handles security-related exceptions that should not expose internals.
     *
     * @param ex security exception
     * @return HTTP 400 response with a generic invalid-request message
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.error("Security violation [{}]: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(correlationId, HttpStatus.BAD_REQUEST.value(), "Invalid request"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Malformed request body [{}]: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(correlationId, HttpStatus.BAD_REQUEST.value(), "Malformed request body"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Unsupported media type [{}]: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(errorBody(correlationId, HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "Unsupported media type"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Method not allowed [{}]: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(errorBody(correlationId, HttpStatus.METHOD_NOT_ALLOWED.value(), "Method not allowed"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.debug("Resource not found [{}]: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody(correlationId, HttpStatus.NOT_FOUND.value(), "Resource not found"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Data integrity violation [{}]: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody(correlationId, HttpStatus.BAD_REQUEST.value(), "Invalid data: a required field is missing or violates constraints"));
    }

    /**
     * Handles any uncaught exception as a fallback.
     *
     * @param ex unexpected exception
     * @return HTTP 500 response with a generic message and correlation id
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.error("Unexpected error [{}]: {}", correlationId, ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(correlationId, HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred"));
    }

    /**
     * Builds the standard error response payload.
     *
     * @param correlationId generated request correlation id
     * @param status HTTP status code
     * @param message human-readable error message
     * @return map representation of the error response body
     */
    private Map<String, Object> errorBody(String correlationId, int status, String message) {
        return Map.of(
                "correlationId", correlationId,
                "status", status,
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
