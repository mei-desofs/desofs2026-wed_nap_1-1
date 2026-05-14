package com.example.desofs.controller;

import com.example.desofs.controllers.AuditLogController;
import com.example.desofs.domain.AuditLog;
import com.example.desofs.domain.Role;
import com.example.desofs.security.RoleGuard;
import com.example.desofs.services.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuditLogController using Spring @WebMvcTest with JWT security.
 */
@WebMvcTest(AuditLogController.class)
@DisplayName("AuditLogController Integration Tests")
class AuditLogControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private RoleGuard roleGuard;

    private AuditLog assignLog;
    private AuditLog removeLog;

    @BeforeEach
    void setUp() {
        assignLog = AuditLog.of("auth0|admin1", "auth0|user1", Role.SUPPORT, "ASSIGN");
        removeLog = AuditLog.of("auth0|admin1", "auth0|user2", Role.CUSTOMER, "REMOVE");
    }

    // ============ AUTHENTICATION Tests ============

    @Test
    @DisplayName("GET /api/audit-logs without JWT should return 401 Unauthorized")
    void testListAuditLogs_WithoutJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(auditLogService, never()).listAll();
    }

    @Test
    @DisplayName("GET /api/audit-logs/{id} without JWT should return 401 Unauthorized")
    void testGetAuditLog_WithoutJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/audit-logs/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ============ AUTHORIZATION Tests ============

    @Test
    @DisplayName("GET /api/audit-logs with non-ADMIN role should return 403 Forbidden")
    void testListAuditLogs_WithNonAdminRole_Returns403() throws Exception {
        doThrow(new AccessDeniedException("Access denied. Required role: ADMIN"))
                .when(roleGuard).requireRole(any(Jwt.class), eq(Role.ADMIN));

        mockMvc.perform(get("/api/audit-logs")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.subject("auth0|customer1"))))
                .andExpect(status().isForbidden());

        verify(auditLogService, never()).listAll();
    }

    @Test
    @DisplayName("GET /api/audit-logs with CUSTOMER role should return 403 Forbidden")
    void testListAuditLogs_WithCustomerRole_Returns403() throws Exception {
        doThrow(new AccessDeniedException("Access denied. Required role: ADMIN"))
                .when(roleGuard).requireRole(any(Jwt.class), eq(Role.ADMIN));

        mockMvc.perform(get("/api/audit-logs")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.subject("auth0|customer1"))))
                .andExpect(status().isForbidden());

        verify(auditLogService, never()).listAll();
    }

    @Test
    @DisplayName("GET /api/audit-logs with SUPPORT role should return 403 Forbidden")
    void testListAuditLogs_WithSupportRole_Returns403() throws Exception {
        doThrow(new AccessDeniedException("Access denied. Required role: ADMIN"))
                .when(roleGuard).requireRole(any(Jwt.class), eq(Role.ADMIN));

        mockMvc.perform(get("/api/audit-logs")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.subject("auth0|support1"))))
                .andExpect(status().isForbidden());

        verify(auditLogService, never()).listAll();
    }

    @Test
    @DisplayName("GET /api/audit-logs/{id} with non-ADMIN role should return 403 Forbidden")
    void testGetAuditLog_WithNonAdminRole_Returns403() throws Exception {
        doThrow(new AccessDeniedException("Access denied. Required role: ADMIN"))
                .when(roleGuard).requireRole(any(Jwt.class), eq(Role.ADMIN));

        mockMvc.perform(get("/api/audit-logs/1")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.subject("auth0|customer1"))))
                .andExpect(status().isForbidden());
    }

    // ============ GET /api/audit-logs (List All) ============

    @Test
    @DisplayName("GET /api/audit-logs with ADMIN should return 200 OK with all entries")
    void testListAuditLogs_AsAdmin_Returns200WithList() throws Exception {
        when(auditLogService.listAll()).thenReturn(List.of(assignLog, removeLog));

        mockMvc.perform(get("/api/audit-logs")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.subject("auth0|admin1"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].actorId").value("auth0|admin1"))
                .andExpect(jsonPath("$[0].targetUserId").value("auth0|user1"))
                .andExpect(jsonPath("$[0].role").value("SUPPORT"))
                .andExpect(jsonPath("$[0].operation").value("ASSIGN"))
                .andExpect(jsonPath("$[1].actorId").value("auth0|admin1"))
                .andExpect(jsonPath("$[1].targetUserId").value("auth0|user2"))
                .andExpect(jsonPath("$[1].role").value("CUSTOMER"))
                .andExpect(jsonPath("$[1].operation").value("REMOVE"));

        verify(auditLogService, times(1)).listAll();
    }

    @Test
    @DisplayName("GET /api/audit-logs with ADMIN and empty store returns empty array")
    void testListAuditLogs_AsAdmin_EmptyStore_Returns200EmptyArray() throws Exception {
        when(auditLogService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/audit-logs")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.subject("auth0|admin1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(auditLogService, times(1)).listAll();
    }

    @Test
    @DisplayName("GET /api/audit-logs response contains all required fields")
    void testListAuditLogs_ResponseContainsAllRequiredFields() throws Exception {
        when(auditLogService.listAll()).thenReturn(List.of(assignLog));

        mockMvc.perform(get("/api/audit-logs")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.subject("auth0|admin1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actorId").exists())
                .andExpect(jsonPath("$[0].targetUserId").exists())
                .andExpect(jsonPath("$[0].role").exists())
                .andExpect(jsonPath("$[0].operation").exists())
                .andExpect(jsonPath("$[0].timestamp").exists());
    }

    // ============ GET /api/audit-logs/{id} ============

    @Test
    @DisplayName("GET /api/audit-logs/{id} with ADMIN returns 404 (not yet implemented)")
    void testGetAuditLog_AsAdmin_Returns404() throws Exception {
        mockMvc.perform(get("/api/audit-logs/1")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.subject("auth0|admin1"))))
                .andExpect(status().isNotFound());
    }

    // ============ SECURITY Tests ============

    @Test
    @DisplayName("GET /api/audit-logs response does not expose sensitive internal fields")
    void testListAuditLogs_ResponseDoesNotExposeSensitiveData() throws Exception {
        when(auditLogService.listAll()).thenReturn(List.of(assignLog));

        mockMvc.perform(get("/api/audit-logs")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.subject("auth0|admin1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].secret").doesNotExist())
                .andExpect(jsonPath("$[0].token").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/audit-logs service is never called when unauthenticated")
    void testListAuditLogs_ServiceNotCalledWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(auditLogService);
    }
}
