package com.example.desofs.exceptions;

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

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        HttpStatus status = HttpStatus.resolve(statusCode != null ? statusCode : 500);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String correlationId = UUID.randomUUID().toString();
        String message = status.series() == HttpStatus.Series.CLIENT_ERROR
                ? status.getReasonPhrase()
                : "An unexpected error occurred";

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "correlationId", correlationId,
                        "status", status.value(),
                        "message", message,
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
}
