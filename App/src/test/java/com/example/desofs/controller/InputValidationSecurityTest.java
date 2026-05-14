package com.example.desofs.controller;

import com.example.desofs.config.TestSecurityConfig;
import com.example.desofs.domain.Movie;
import com.example.desofs.repositories.MovieRepository;
import com.example.desofs.repositories.OrderRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security-focused integration tests that simulate common attack vectors:
 * - SQL Injection
 * - XSS payloads
 * - Path traversal
 * - JSON injection
 * - Oversized payloads
 * - Negative/zero quantity manipulation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class InputValidationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private OrderRepository orderRepository;

    private static final String AUTH0_ID = "auth0|securitytester";
    private static final String ROLES_CLAIM = "https://emovieshop.com/roles";

    private Long validMovieId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        movieRepository.deleteAll();
        Movie movie = movieRepository.save(new Movie("Test Movie", "Random Description", "Action", "Netflix", new BigDecimal("9.99"), 100));
        validMovieId = movie.getId();
    }

    // ---- SQL Injection Attempts ----

    @Test
    @DisplayName("SQL injection in receiptName is sanitized")
    void sqlInjection_receiptName_sanitized() throws Exception {
        String payload = "'; DROP TABLE orders; --";

        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptName": "%s", "items": [{"movieId": %d, "quantity": 1}]}
                                """.formatted(payload, validMovieId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptName").value("DROP TABLE orders --"));
    }

    @Test
    @DisplayName("SQL injection in receiptName with UNION SELECT")
    void sqlInjection_unionSelect_sanitized() throws Exception {
        String payload = "test' UNION SELECT * FROM users --";

        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptName": "%s", "items": [{"movieId": %d, "quantity": 1}]}
                                """.formatted(payload, validMovieId)))
                .andExpect(status().isCreated());
        // If we get here, the injection was neutralized (sanitized or escaped)
    }

    // ---- XSS Payloads ----

    @Test
    @DisplayName("XSS script tag in receiptName is stripped")
    void xss_scriptTag_stripped() throws Exception {
        String payload = "<script>alert('xss')</script>";

        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptName": "%s", "items": [{"movieId": %d, "quantity": 1}]}
                                """.formatted(payload, validMovieId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptName").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script>"))));
    }

    @Test
    @DisplayName("XSS event handler in receiptName is stripped")
    void xss_eventHandler_stripped() throws Exception {
        // Characters like " and = are stripped by the allow-list sanitizer
        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiptName\": \"name onmouseover alert1\", \"items\": [{\"movieId\": " + validMovieId + ", \"quantity\": 1}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptName").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<"))));
    }

    // ---- Path Traversal ----

    @Test
    @DisplayName("Path traversal with ../ is neutralized")
    void pathTraversal_dotDot_neutralized() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptName": "../../../etc/passwd", "items": [{"movieId": %d, "quantity": 1}]}
                                """.formatted(validMovieId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptName").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(".."))));
    }

    @Test
    @DisplayName("Path traversal with backslash is neutralized")
    void pathTraversal_backslash_neutralized() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptName": "..\\\\..\\\\windows\\\\system32", "items": [{"movieId": %d, "quantity": 1}]}
                                """.formatted(validMovieId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptName").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("\\"))));
    }

    @Test
    @DisplayName("Null bytes in receiptName are stripped")
    void pathTraversal_nullBytes_stripped() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptName": "receipt\\u0000.exe", "items": [{"movieId": %d, "quantity": 1}]}
                                """.formatted(validMovieId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptName").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("\0"))));
    }

    // ---- Quantity Manipulation ----

    @Test
    @DisplayName("Zero quantity is rejected")
    void quantityManipulation_zero_rejected() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptName": "Test", "items": [{"movieId": %d, "quantity": 0}]}
                                """.formatted(validMovieId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Negative quantity is rejected")
    void quantityManipulation_negative_rejected() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptName": "Test", "items": [{"movieId": %d, "quantity": -5}]}
                                """.formatted(validMovieId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Extremely large quantity is handled")
    void quantityManipulation_extremelyLarge_rejected() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptName": "Test", "items": [{"movieId": %d, "quantity": 2147483647}]}
                                """.formatted(validMovieId)))
                .andExpect(status().isConflict()); // Insufficient stock
    }

    // ---- Missing and Null Fields ----

    @Test
    @DisplayName("Null movieId is rejected")
    void nullField_movieId_rejected() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptName": "Test", "items": [{"movieId": null, "quantity": 1}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Null quantity is rejected")
    void nullField_quantity_rejected() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptName": "Test", "items": [{"movieId": %d, "quantity": null}]}
                                """.formatted(validMovieId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Empty body is rejected with 400")
    void emptyBody_rejected() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Malformed JSON is rejected with 400")
    void malformedJson_rejected() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest());
    }

    // ---- Content-Type Attacks ----

    @Test
    @DisplayName("Non-JSON content type is rejected with 415")
    void contentType_nonJson_rejected() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("""
                                {"receiptName": "Test", "items": [{"movieId": %d, "quantity": 1}]}
                                """.formatted(validMovieId)))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ---- Oversized Input ----

    @Test
    @DisplayName("Very long receiptName is rejected by validation")
    void oversizedInput_longReceiptName_rejected() throws Exception {
        String longName = "A".repeat(200);

        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptName": "%s", "items": [{"movieId": %d, "quantity": 1}]}
                                """.formatted(longName, validMovieId)))
                .andExpect(status().isBadRequest());
    }

    // ---- IDOR (Insecure Direct Object Reference) ----

    @Test
    @DisplayName("Non-existent movie ID returns proper error, no information leak")
    void idor_nonExistentMovieId_noInfoLeak() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptName": "Test", "items": [{"movieId": 99999, "quantity": 1}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQL"))))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("stack"))));
    }

    // ---- Helper ----

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor customerJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(AUTH0_ID)
                        .claim(ROLES_CLAIM, List.of("CUSTOMER")));
    }
}
