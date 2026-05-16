package com.example.desofs.controller;

import com.example.desofs.controllers.OrderController;
import com.example.desofs.domain.Role;
import com.example.desofs.security.IRoleGuard;
import com.example.desofs.services.IOrderService;
import com.example.desofs.shared.dtos.OrderItemResponseDTO;
import com.example.desofs.shared.dtos.OrderResponseDTO;
import com.example.desofs.shared.dtos.PurchaseRequestDTO;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrderController using Spring @WebMvcTest with JWT security.
 */
@WebMvcTest(OrderController.class)
@DisplayName("OrderController Integration Tests")
class OrderControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IOrderService orderService;

    @MockitoBean
    private IRoleGuard roleGuard;

    private OrderResponseDTO testOrderResponse;
    private String validRequestBody;

    @BeforeEach
    void setUp() {
        List<OrderItemResponseDTO> items = List.of(
                new OrderItemResponseDTO(1L, "Inception", 2, new BigDecimal("14.99"),
                        new BigDecimal("29.98"))
        );
        testOrderResponse = new OrderResponseDTO(
                1L,
                "COMPLETED",
                "My Receipt",
                new BigDecimal("29.98"),
                LocalDateTime.now(),
                items
        );

        validRequestBody = "{"
                + "\"items\":["
                + "  {\"movieId\":1,\"quantity\":2}"
                + "],"
                + "\"receiptName\":\"My Receipt\""
                + "}";
    }

    // ============ AUTHENTICATION Tests ============

    @Test
    @DisplayName("POST /api/orders without JWT should return 401 Unauthorized")
    void testCreateOrder_WithoutJwt_Returns401() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody))
                .andExpect(status().isUnauthorized());

        verify(orderService, never()).createOrder(any(), any());
    }

    @Test
    @DisplayName("POST /api/orders with valid JWT and CUSTOMER role should return 201")
    void testCreateOrder_WithValidJwt_AndCustomerRole_Returns201() throws Exception {
        when(orderService.createOrder(eq("auth0|user123"), any(PurchaseRequestDTO.class)))
                .thenReturn(testOrderResponse);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("auth0|user123"))
                                .authorities(auth -> List.of())
                        ))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1L))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.receiptName").value("My Receipt"))
                .andExpect(jsonPath("$.totalPrice").value("29.98"));

        verify(orderService, times(1))
                .createOrder(eq("auth0|user123"), any(PurchaseRequestDTO.class));
    }

    // ============ AUTHORIZATION Tests ============

    @Test
    @DisplayName("POST /api/orders with non-CUSTOMER role should return 403 Forbidden")
    void testCreateOrder_WithNonCustomerRole_Returns403() throws Exception {
        doThrow(new AccessDeniedException("User does not have CUSTOMER role"))
                .when(roleGuard).requireRole(any(Jwt.class), eq(Role.CUSTOMER));

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("auth0|admin123"))
                        ))
                .andExpect(status().isForbidden());

        verify(orderService, never()).createOrder(any(), any());
    }

    // ============ REQUEST VALIDATION Tests ============

    @Test
    @DisplayName("POST /api/orders with invalid payload should return 400 Bad Request")
    void testCreateOrder_WithMalformedJson_Returns400() throws Exception {
        String invalidRequestBody = "{\"items\":[{\"movieId\":invalid}]}";

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("auth0|user123"))
                        ))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(any(), any());
    }

    @Test
    @DisplayName("POST /api/orders with empty items should return 400")
    void testCreateOrder_WithEmptyItems_Returns400() throws Exception {
        String emptyItemsRequest = "{"
                + "\"items\":[],"
                + "\"receiptName\":\"Empty\""
                + "}";

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyItemsRequest)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("auth0|user123"))
                        ))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(any(), any());
    }

    @Test
    @DisplayName("POST /api/orders with negative quantity should return 400")
    void testCreateOrder_WithNegativeQuantity_Returns400() throws Exception {
        String negativeQtyRequest = "{"
                + "\"items\":[{\"movieId\":1,\"quantity\":-5}],"
                + "\"receiptName\":\"Test\""
                + "}";

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(negativeQtyRequest)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("auth0|user123"))
                        ))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(any(), any());
    }

    @Test
    @DisplayName("POST /api/orders with exceeding item count returns business error")
    void testCreateOrder_ExceedingItemLimit_Returns400() throws Exception {
        when(orderService.createOrder(anyString(), any(PurchaseRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Cannot purchase more than 10 movies"));

        String tooManyItemsRequest = "{"
                + "\"items\":["
                + "  {\"movieId\":1,\"quantity\":1},"
                + "  {\"movieId\":2,\"quantity\":1},"
                + "  {\"movieId\":3,\"quantity\":1},"
                + "  {\"movieId\":4,\"quantity\":1},"
                + "  {\"movieId\":5,\"quantity\":1},"
                + "  {\"movieId\":6,\"quantity\":1},"
                + "  {\"movieId\":7,\"quantity\":1},"
                + "  {\"movieId\":8,\"quantity\":1},"
                + "  {\"movieId\":9,\"quantity\":1},"
                + "  {\"movieId\":10,\"quantity\":1},"
                + "  {\"movieId\":11,\"quantity\":1}"
                + "],"
                + "\"receiptName\":\"Too Many\""
                + "}";

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tooManyItemsRequest)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("auth0|user123"))
                        ))
                .andExpect(status().isBadRequest());
    }

    // ============ RESPONSE VALIDATION Tests ============

    @Test
    @DisplayName("POST /api/orders response contains correct order details")
    void testCreateOrder_ResponseContainsAllRequiredFields() throws Exception {
        when(orderService.createOrder(eq("auth0|user123"), any(PurchaseRequestDTO.class)))
                .thenReturn(testOrderResponse);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("auth0|user123"))
                        ))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.receiptName").exists())
                .andExpect(jsonPath("$.totalPrice").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].movieId").value(1L))
                .andExpect(jsonPath("$.items[0].movieTitle").value("Inception"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value("14.99"))
                .andExpect(jsonPath("$.items[0].subtotal").value("29.98"));
    }

    @Test
    @DisplayName("POST /api/orders response status code is 201 CREATED")
    void testCreateOrder_ReturnsCorrectStatusCode() throws Exception {
        when(orderService.createOrder(eq("auth0|user123"), any(PurchaseRequestDTO.class)))
                .thenReturn(testOrderResponse);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("auth0|user123"))
                        ))
                .andExpect(status().isCreated());
    }

    // ============ SECURITY: Price & Stock Tests ============

    @Test
    @DisplayName("POST /api/orders verifies that price comes from database")
    void testCreateOrder_PriceVerification() throws Exception {
        String requestWithWrongPrice = "{"
                + "\"items\":[{\"movieId\":1,\"quantity\":1,\"clientPrice\":1.00}],"
                + "\"receiptName\":\"Price Test\""
                + "}";

        when(orderService.createOrder(eq("auth0|user123"), any(PurchaseRequestDTO.class)))
                .thenReturn(testOrderResponse);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithWrongPrice)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("auth0|user123"))
                        ))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].unitPrice").value("14.99"));
    }

    @Test
    @DisplayName("POST /api/orders with movie not found throws IllegalArgumentException")
    void testCreateOrder_MovieNotFound_Returns400() throws Exception {
        when(orderService.createOrder(eq("auth0|user123"), any(PurchaseRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Movie not found: 999"));

        String requestWithInvalidMovie = "{"
                + "\"items\":[{\"movieId\":999,\"quantity\":1}],"
                + "\"receiptName\":\"Not Found\""
                + "}";

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithInvalidMovie)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("auth0|user123"))
                        ))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/orders with insufficient stock returns 409 Conflict")
    void testCreateOrder_InsufficientStock_Returns409() throws Exception {
        when(orderService.createOrder(eq("auth0|user123"), any(PurchaseRequestDTO.class)))
                .thenThrow(new IllegalStateException("Insufficient stock for movie 1"));

        String requestWithHighQty = "{"
                + "\"items\":[{\"movieId\":1,\"quantity\":1000}],"
                + "\"receiptName\":\"Out of Stock\""
                + "}";

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithHighQty)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("auth0|user123"))
                        ))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/orders detects and rejects duplicate movie IDs")
    void testCreateOrder_DuplicateMovieIDs_Returns400() throws Exception {
        when(orderService.createOrder(eq("auth0|user123"), any(PurchaseRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Duplicate movie IDs are not allowed"));

        String duplicateMoviesRequest = "{"
                + "\"items\":["
                + "  {\"movieId\":1,\"quantity\":2},"
                + "  {\"movieId\":1,\"quantity\":1}"
                + "],"
                + "\"receiptName\":\"Duplicate Test\""
                + "}";

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateMoviesRequest)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("auth0|user123"))
                        ))
                .andExpect(status().isBadRequest());
    }

    // ============ AUTHENTICATION DETAILS Tests ============

    @Test
    @DisplayName("POST /api/orders extracts user ID from JWT subject claim")
    void testCreateOrder_UsesJwtSubjectAsAuth0Id() throws Exception {
        String expectedAuth0Id = "auth0|xyz789";

        when(orderService.createOrder(eq(expectedAuth0Id), any(PurchaseRequestDTO.class)))
                .thenReturn(testOrderResponse);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(expectedAuth0Id))
                        ))
                .andExpect(status().isCreated());

        verify(orderService, times(1))
                .createOrder(eq(expectedAuth0Id), any(PurchaseRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/orders works with different JWT subjects")
    void testCreateOrder_MultipleUsers() throws Exception {
        String user1 = "auth0|user1";
        String user2 = "auth0|user2";
        OrderResponseDTO response1 = new OrderResponseDTO(
                1L, "COMPLETED", "R1", new BigDecimal("10.00"), LocalDateTime.now(), List.of());
        OrderResponseDTO response2 = new OrderResponseDTO(
                2L, "COMPLETED", "R2", new BigDecimal("20.00"), LocalDateTime.now(), List.of());

        when(orderService.createOrder(eq(user1), any(PurchaseRequestDTO.class)))
                .thenReturn(response1);
        when(orderService.createOrder(eq(user2), any(PurchaseRequestDTO.class)))
                .thenReturn(response2);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody)
                        .with(jwt().jwt(jwt -> jwt.subject(user1)))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1L));

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody)
                        .with(jwt().jwt(jwt -> jwt.subject(user2)))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(2L));

        verify(orderService, times(1)).createOrder(eq(user1), any());
        verify(orderService, times(1)).createOrder(eq(user2), any());
    }
}