package com.example.desofs.controller;


import com.example.desofs.controllers.CustomErrorController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomErrorController Tests")
class CustomErrorControllerTest {

    private final CustomErrorController customErrorController = new CustomErrorController();

    @Test
    @DisplayName("/error without status code should default to 404 JSON")
    void handleError_defaultsTo404() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        var response = customErrorController.handleError(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).containsEntry("status", 404)
                .containsEntry("message", "Resource not found");
    }

    @Test
    @DisplayName("/error with 401 should return Unauthorized JSON")
    void handleError_unauthorized() throws Exception {
        assertErrorResponse(401, "Unauthorized");
    }

    @Test
    @DisplayName("/error with 403 should return Access denied JSON")
    void handleError_forbidden() throws Exception {
        assertErrorResponse(403, "Access denied");
    }

    @Test
    @DisplayName("/error with 422 should return Bad request JSON")
    void handleError_badRequestFamily() throws Exception {
        assertErrorResponse(422, "Bad request");
    }

    @Test
    @DisplayName("/error with 500 should return generic error JSON")
    void handleError_serverError() throws Exception {
        assertErrorResponse(500, "An unexpected error occurred");
    }

    private void assertErrorResponse(int statusCode, String expectedMessage) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("jakarta.servlet.error.status_code", statusCode);

        var response = customErrorController.handleError(request);

        assertThat(response.getStatusCode().value()).isEqualTo(statusCode);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsEntry("status", statusCode)
                .containsEntry("message", expectedMessage);
        assertThat(body.get("correlationId").toString()).matches("[0-9a-fA-F\\-]{36}");
        assertThat(body.get("timestamp").toString()).isNotBlank();
    }
}
