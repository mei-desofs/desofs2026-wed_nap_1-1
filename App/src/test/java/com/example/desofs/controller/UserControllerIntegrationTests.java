package com.example.desofs.controller;

import com.example.desofs.config.SecurityConfig;
import com.example.desofs.controllers.UserController;
import com.example.desofs.domain.Role;
import com.example.desofs.security.IRoleGuard;
import com.example.desofs.services.IAuditLogService;
import com.example.desofs.services.ITokenInvalidationService;
import com.example.desofs.services.IUserService;
import com.example.desofs.shared.dtos.UserDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTests {

    private static final String ACTOR_SUB = "auth0|admin-1";
    private static final String TARGET_ID = "auth0|user-2";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUserService userService;

    @MockitoBean
    private IRoleGuard roleGuard;

    @MockitoBean
    private IAuditLogService auditLogService;

    @MockitoBean
    private ITokenInvalidationService tokenInvalidationService;

    @Test
    @DisplayName("GET /api/users returns the user list and enforces ADMIN role")
    void getUsers_returnsList() throws Exception {
        UserDTO admin = new UserDTO("auth0|1", "a@b.c", "Admin", List.of("ADMIN"));
        UserDTO support = new UserDTO("auth0|2", "s@b.c", "Support", List.of("SUPPORT"));
        when(userService.getAllUsers()).thenReturn(List.of(admin, support));

        mockMvc.perform(get("/api/users")
                        .with(jwt().jwt(j -> j.subject(ACTOR_SUB)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId").value("auth0|1"))
                .andExpect(jsonPath("$[1].userId").value("auth0|2"));

        verify(roleGuard).requireRole(any(), eq(Role.ADMIN));
        verify(userService).getAllUsers();
    }

    @Test
    @DisplayName("POST /api/users/{id}/roles assigns the role and returns 204")
    void assignRole_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/users/{id}/roles", TARGET_ID)
                        .with(jwt().jwt(j -> j.subject(ACTOR_SUB)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPPORT\"}"))
                .andExpect(status().isNoContent());

        verify(roleGuard).requireRole(any(), eq(Role.ADMIN));
        verify(userService).assignRole(ACTOR_SUB, TARGET_ID, Role.SUPPORT);
    }

    @Test
    @DisplayName("DELETE /api/users/{id}/roles removes the role and returns 204")
    void removeRole_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/{id}/roles", TARGET_ID)
                        .with(jwt().jwt(j -> j.subject(ACTOR_SUB)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isNoContent());

        verify(roleGuard).requireRole(any(), eq(Role.ADMIN));
        verify(userService).removeRole(ACTOR_SUB, TARGET_ID, Role.CUSTOMER);
    }
}
