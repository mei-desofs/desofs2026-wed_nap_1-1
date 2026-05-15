package com.example.desofs.controllers;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Custom error controller that ensures all error responses are returned as JSON.
 * <p>
 * Replaces Spring Boot's default BasicErrorController which returns HTML for
 * non-API clients, preventing "Unexpected Content-Type" findings in security scans.
 */
@RestController
public class CustomErrorController implements ErrorController {

    @SuppressFBWarnings(value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
            justification = "Error controller must handle forwards from all HTTP methods; CSRF is disabled for this stateless JWT API")
    @RequestMapping(value = "/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        if (statusCode == null) {
            statusCode = HttpStatus.NOT_FOUND.value();
        }

        String message;
        if (statusCode == 404) {
            message = "Resource not found";
        } else if (statusCode == 401) {
            message = "Unauthorized";
        } else if (statusCode == 403) {
            message = "Access denied";
        } else if (statusCode >= 400 && statusCode < 500) {
            message = "Bad request";
        } else {
            message = "An unexpected error occurred";
        }

        String correlationId = UUID.randomUUID().toString();

        Map<String, Object> body = Map.of(
                "correlationId", correlationId,
                "status", statusCode,
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        );

        return ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
