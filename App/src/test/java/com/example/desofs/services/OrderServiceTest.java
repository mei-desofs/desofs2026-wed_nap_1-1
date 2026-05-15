package com.example.desofs.services;

import com.example.desofs.domain.Movie;
import com.example.desofs.domain.Order;
import com.example.desofs.repositories.MovieRepository;
import com.example.desofs.repositories.OrderRepository;
import com.example.desofs.shared.dtos.OrderResponseDTO;
import com.example.desofs.shared.dtos.PurchaseItemDTO;
import com.example.desofs.shared.dtos.PurchaseRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private ReceiptFileService receiptFileService;
    @Mock
    private com.example.desofs.shared.mappers.IOrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Movie testMovie;
    private static final String AUTH0_ID = "auth0|user123";

    @BeforeEach
    void setUp() {
        testMovie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Netflix", new BigDecimal("14.99"), 10);
        setId(testMovie, 1L);
    }

    private void setupOrderResponseMapper() {
        when(orderMapper.toResponseDTO(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            var items = o.getItems().stream().map(it -> new com.example.desofs.shared.dtos.OrderItemResponseDTO(
                    it.getMovie().getId(), it.getMovie().getTitle(), it.getQuantity(), it.getUnitPrice(), it.getSubtotal()
            )).toList();
            return new OrderResponseDTO(o.getId(), o.getStatus().toString(), o.getReceiptName(), o.getTotalPrice(), o.getCreatedAt(), items);
        });
    }

        // OPAQUE-BOX TESTS : Focus on the external behavior of the class, without knowing how it is implemented.
        @Nested
        class OpaqueBoxTests {

        // ---- Validation and behavior scenarios ----

        @Test
        @DisplayName("More than 10 items throws exception")
        void createOrder_tooManyItems_throws() {
            List<PurchaseItemDTO> items = new java.util.ArrayList<>();
            for (int i = 1; i <= 11; i++) {
            items.add(new PurchaseItemDTO((long) i, 1));
            }
            PurchaseRequestDTO request = new PurchaseRequestDTO("Receipt", items);

            assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(AUTH0_ID, request));
        }

        @Test
        @DisplayName("Duplicate movie IDs throws exception")
        void createOrder_duplicateMovieIds_throws() {
            PurchaseRequestDTO request = new PurchaseRequestDTO(
                "Receipt",
                List.of(new PurchaseItemDTO(1L, 1), new PurchaseItemDTO(1L, 2))
            );

            assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(AUTH0_ID, request));
        }

        @Test
        @DisplayName("Non-existent movie throws exception")
        void createOrder_movieNotFound_throws() {
            PurchaseRequestDTO request = new PurchaseRequestDTO(
                "Receipt",
                List.of(new PurchaseItemDTO(999L, 1))
            );

            when(receiptFileService.sanitizeReceiptName("Receipt")).thenReturn("Receipt");
            when(movieRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(AUTH0_ID, request));
        }

        @Test
        @DisplayName("Insufficient stock throws exception")
        void createOrder_insufficientStock_throws() {
            Movie lowStockMovie = new Movie("Rare Movie", "A rare drama", "Drama", "HBO", new BigDecimal("19.99"), 1);
            setId(lowStockMovie, 2L);

            PurchaseRequestDTO request = new PurchaseRequestDTO(
                "Receipt",
                List.of(new PurchaseItemDTO(2L, 5))
            );

            when(receiptFileService.sanitizeReceiptName("Receipt")).thenReturn("Receipt");
            when(movieRepository.findById(2L)).thenReturn(Optional.of(lowStockMovie));

            assertThrows(IllegalStateException.class,
                () -> orderService.createOrder(AUTH0_ID, request));
        }

        @Test
        @DisplayName("Invalid receipt name throws exception")
        void createOrder_invalidReceiptName_throws() {
            PurchaseRequestDTO request = new PurchaseRequestDTO(
                "!@#$%",
                List.of(new PurchaseItemDTO(1L, 1))
            );

            when(receiptFileService.sanitizeReceiptName("!@#$%"))
                .thenThrow(new IllegalArgumentException("Receipt name contains no valid characters after sanitization"));

            assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(AUTH0_ID, request));
        }

        @Test
        @DisplayName("Price is resolved from database, not from client")
        void createOrder_priceFromDb_notClient() throws Exception {
            setupOrderResponseMapper();
            PurchaseRequestDTO request = new PurchaseRequestDTO(
                "Receipt",
                List.of(new PurchaseItemDTO(1L, 1))
            );

            when(receiptFileService.sanitizeReceiptName("Receipt")).thenReturn("Receipt");
            when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            setId(o, 101L);
            return o;
            });

            OrderResponseDTO response = orderService.createOrder(AUTH0_ID, request);

            // Price should match the DB movie price (14.99), not any client-supplied value
            assertEquals(new BigDecimal("14.99"), response.items().get(0).unitPrice());
        }

        @Test
        @DisplayName("Receipt file failure does not prevent order creation")
        void createOrder_receiptFileFailure_orderStillCreated() throws Exception {
            setupOrderResponseMapper();
            PurchaseRequestDTO request = new PurchaseRequestDTO(
                "Receipt",
                List.of(new PurchaseItemDTO(1L, 1))
            );

            when(receiptFileService.sanitizeReceiptName("Receipt")).thenReturn("Receipt");
            when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            setId(o, 102L);
            return o;
            });
            doThrow(new java.io.IOException("Disk full"))
                .when(receiptFileService).createReceiptFile(any(Order.class));

            // Should not throw - order is still created
            OrderResponseDTO response = orderService.createOrder(AUTH0_ID, request);
            assertNotNull(response);
            assertEquals(102L, response.orderId());
        }
        }

        // TRANSPARENT-BOX TESTS : Inspect or manipulate internal state or behavior
        @Nested
        class TransparentBoxTests {

        // ---- Success scenarios with interaction verifications ----
        @Test
        @DisplayName("Valid purchase creates order and returns response")
        void createOrder_validRequest_success() throws Exception {
            setupOrderResponseMapper();
            PurchaseRequestDTO request = new PurchaseRequestDTO(
                "Customer Receipt",
                List.of(new PurchaseItemDTO(1L, 2))
            );

            when(receiptFileService.sanitizeReceiptName("Customer Receipt")).thenReturn("Customer Receipt");
            when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            setId(o, 100L);
            return o;
            });

            OrderResponseDTO response = orderService.createOrder(AUTH0_ID, request);

            assertNotNull(response);
            assertEquals(100L, response.orderId());
            assertEquals("COMPLETED", response.status());
            assertEquals(1, response.items().size());
            assertEquals(new BigDecimal("29.98"), response.totalPrice());

            verify(receiptFileService).createReceiptFile(any(Order.class));
        }
        }

    // ---- Helper ----

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}

