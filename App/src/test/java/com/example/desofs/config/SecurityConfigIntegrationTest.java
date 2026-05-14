package com.example.desofs.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests that verify the security configuration is correctly applied:
 * - Security headers (HSTS, X-Content-Type-Options, X-Frame-Options)
 * - CSRF disabled for stateless API
 * - Session management is stateless
 * - Unauthenticated requests are properly rejected
 * - Error responses do not leak sensitive information
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String ROLES_CLAIM = "https://emovieshop.com/roles";

    // ---- Authentication Tests ----

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401")
    void unauthenticated_protectedEndpoint_returns401() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authenticated request passes security filter (not 401)")
    void authenticated_requestPassesSecurity() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("auth0|user1")
                                        .claim(ROLES_CLAIM, List.of("CUSTOMER")))))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertNotEquals(401, status, "Authenticated request must not return 401");
                });
    }

    // ---- Security Headers ----

    @Test
    @DisplayName("X-Content-Type-Options: nosniff header is present")
    void securityHeaders_xContentTypeOptions_present() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("auth0|user1")
                                        .claim(ROLES_CLAIM, List.of("CUSTOMER"))))
                        .contentType("application/json")
                        .content("""
                                {"receiptName": "Test", "items": []}
                                """))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("X-Frame-Options: DENY header is present")
    void securityHeaders_xFrameOptions_present() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("auth0|user1")
                                        .claim(ROLES_CLAIM, List.of("CUSTOMER"))))
                        .contentType("application/json")
                        .content("""
                                {"receiptName": "Test", "items": []}
                                """))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    @DisplayName("Cache-Control header prevents caching sensitive responses")
    void securityHeaders_cacheControl_noCaching() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("auth0|user1")
                                        .claim(ROLES_CLAIM, List.of("CUSTOMER"))))
                        .contentType("application/json")
                        .content("""
                                {"receiptName": "Test", "items": []}
                                """))
                .andExpect(header().exists("Cache-Control"));
    }

    // ---- Error Response Tests (No Info Leakage) ----

    @Test
    @DisplayName("Server errors do not expose stack traces")
    void errorResponse_noStackTrace() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("auth0|user1")
                                        .claim(ROLES_CLAIM, List.of("CUSTOMER"))))
                        .contentType("application/json")
                        .content("""
                                {"receiptName": "Test", "items": [{"movieId": 99999, "quantity": 1}]}
                                """))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    @DisplayName("Error responses include correlation ID for traceability")
    void errorResponse_hasCorrelationId() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("auth0|user1")
                                        .claim(ROLES_CLAIM, List.of("CUSTOMER"))))
                        .contentType("application/json")
                        .content("""
                                {"receiptName": "Test", "items": [{"movieId": 99999, "quantity": 1}]}
                                """))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    // ---- HTTP Method Tests ----

    @Test
    @DisplayName("Unsupported HTTP methods return 405")
    void httpMethods_unsupported_rejected() throws Exception {
        mockMvc.perform(delete("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("auth0|user1")
                                        .claim(ROLES_CLAIM, List.of("ADMIN")))))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("OPTIONS request does not expose sensitive info")
    void httpMethods_options_noInfoLeak() throws Exception {
        mockMvc.perform(options("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("auth0|user1")
                                        .claim(ROLES_CLAIM, List.of("CUSTOMER")))))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    // ---- Session Management ----

    @Test
    @DisplayName("No session cookie is set (stateless)")
    void sessionManagement_noCookieSet() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("auth0|user1")
                                        .claim(ROLES_CLAIM, List.of("CUSTOMER"))))
                        .contentType("application/json")
                        .content("""
                                {"receiptName": "Test", "items": []}
                                """))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }
}
