package com.example.desofs.controller;

import com.example.desofs.controllers.RefundController;
import com.example.desofs.domain.Role;
import com.example.desofs.security.IRoleGuard;
import com.example.desofs.services.IRefundService;
import com.example.desofs.shared.dtos.CreateRefundRequest;
import com.example.desofs.shared.dtos.RefundRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RefundController.class)
@DisplayName("RefundController Integration Tests")
class RefundControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IRefundService refundService;

    @MockitoBean
    private IRoleGuard roleGuard;

    private RefundRequestDTO refund1;
    private RefundRequestDTO refund2;
    private String createRefundRequestBody;

    @BeforeEach
    void setUp() {
        refund1 = new RefundRequestDTO(
            1L,
            10L,
            "auth0|user123",
            new BigDecimal("15.00"),
            "REQUESTED",
            "Not satisfied",
            LocalDateTime.of(2026, 5, 14, 10, 0),
            LocalDateTime.of(2026, 5, 14, 10, 0)
        );

        refund2 = new RefundRequestDTO(
            2L,
            11L,
            "auth0|user456",
            new BigDecimal("20.00"),
            "APPROVED",
            "Duplicate purchase",
            LocalDateTime.of(2026, 5, 13, 9, 30),
            LocalDateTime.of(2026, 5, 13, 12, 15)
        );

        createRefundRequestBody = "{" +
            "\"orderId\":10," +
            "\"amount\":15.00," +
            "\"reason\":\"Not satisfied\"" +
            "}";
    }

    @Test
    @DisplayName("GET /api/refunds should return 200 OK with all refunds")
    void list_returnsAllRefunds() throws Exception {
        when(refundService.listAll()).thenReturn(List.of(refund1, refund2));

        mockMvc.perform(get("/api/refunds")
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].status").value("REQUESTED"))
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].status").value("APPROVED"));

        verify(refundService, times(1)).listAll();
        verifyNoInteractions(roleGuard);
    }

    @Test
    @DisplayName("GET /api/refunds/{id} should return 200 OK when found")
    void get_returnsRefundWhenFound() throws Exception {
        when(refundService.get(1L)).thenReturn(Optional.of(refund1));

        mockMvc.perform(get("/api/refunds/1")
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.orderId").value(10L))
            .andExpect(jsonPath("$.status").value("REQUESTED"));

        verify(refundService, times(1)).get(1L);
    }

    @Test
    @DisplayName("GET /api/refunds/{id} should return 404 when not found")
    void get_returns404WhenMissing() throws Exception {
        when(refundService.get(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/refunds/999")
                .with(jwt()))
            .andExpect(status().isNotFound());

        verify(refundService, times(1)).get(999L);
    }

    @Test
    @DisplayName("POST /api/refunds should create a refund request and return 201")
    void create_returns201AndLocationHeader() throws Exception {
        RefundRequestDTO created = new RefundRequestDTO(
            3L,
            10L,
            "auth0|user123",
            new BigDecimal("15.00"),
            "REQUESTED",
            "Not satisfied",
            LocalDateTime.of(2026, 5, 14, 11, 0),
            LocalDateTime.of(2026, 5, 14, 11, 0)
        );

        when(refundService.createRefundRequest(eq("auth0|user123"), any(CreateRefundRequest.class)))
            .thenReturn(created);

        mockMvc.perform(post("/api/refunds")
                .with(csrf())
                .with(jwt().jwt(jwt -> jwt.subject("auth0|user123")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRefundRequestBody))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/refunds/3"))
            .andExpect(jsonPath("$.id").value(3L))
            .andExpect(jsonPath("$.orderId").value(10L))
            .andExpect(jsonPath("$.status").value("REQUESTED"));

        verify(roleGuard, times(1)).requireRole(any(Jwt.class), eq(Role.CUSTOMER));
        verify(refundService, times(1)).createRefundRequest(eq("auth0|user123"), any(CreateRefundRequest.class));
    }

    @Test
    @DisplayName("POST /api/refunds with non-CUSTOMER role should return 403")
    void create_withWrongRole_returns403() throws Exception {
        doThrow(new AccessDeniedException("User does not have CUSTOMER role"))
            .when(roleGuard).requireRole(any(Jwt.class), eq(Role.CUSTOMER));

        mockMvc.perform(post("/api/refunds")
                .with(csrf())
                .with(jwt().jwt(jwt -> jwt.subject("auth0|admin123")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRefundRequestBody))
            .andExpect(status().isForbidden());

        verify(refundService, never()).createRefundRequest(any(), any());
    }

    @Test
    @DisplayName("PUT /api/refunds/{id}/approve should return 200 when approved")
    void approve_returns200WhenFound() throws Exception {
        when(refundService.approve(1L)).thenReturn(refund2);

        mockMvc.perform(put("/api/refunds/1/approve")
                .with(csrf())
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(2L))
            .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(refundService, times(1)).approve(1L);
        verifyNoInteractions(roleGuard);
    }

    @Test
    @DisplayName("PUT /api/refunds/{id}/approve should return 404 when missing")
    void approve_returns404WhenMissing() throws Exception {
        when(refundService.approve(999L)).thenReturn(null);

        mockMvc.perform(put("/api/refunds/999/approve")
                .with(csrf())
                .with(jwt()))
            .andExpect(status().isNotFound());

        verify(refundService, times(1)).approve(999L);
    }

    @Test
    @DisplayName("PUT /api/refunds/{id}/reject should return 200 when rejected")
    void reject_returns200WhenFound() throws Exception {
        RefundRequestDTO rejected = new RefundRequestDTO(
            4L,
            10L,
            "auth0|admin",
            new BigDecimal("15.00"),
            "REJECTED",
            "Duplicate order",
            LocalDateTime.of(2026, 5, 14, 11, 30),
            LocalDateTime.of(2026, 5, 14, 11, 45)
        );

        when(refundService.reject(1L, "Duplicate order")).thenReturn(rejected);

        mockMvc.perform(put("/api/refunds/1/reject")
                .with(csrf())
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Duplicate order\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(4L))
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.reason").value("Duplicate order"));

        verify(refundService, times(1)).reject(1L, "Duplicate order");
        verifyNoInteractions(roleGuard);
    }

    @Test
    @DisplayName("PUT /api/refunds/{id}/reject should return 404 when missing")
    void reject_returns404WhenMissing() throws Exception {
        when(refundService.reject(999L, "Duplicate order")).thenReturn(null);

        mockMvc.perform(put("/api/refunds/999/reject")
                .with(csrf())
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Duplicate order\"}"))
            .andExpect(status().isNotFound());

        verify(refundService, times(1)).reject(999L, "Duplicate order");
    }
}
