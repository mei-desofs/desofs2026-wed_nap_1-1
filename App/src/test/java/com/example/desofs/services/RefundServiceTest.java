package com.example.desofs.services;

import com.example.desofs.domain.Order;
import com.example.desofs.domain.OrderStatus;
import com.example.desofs.domain.RefundRequest;
import com.example.desofs.repositories.OrderRepository;
import com.example.desofs.repositories.RefundRequestRepository;
import com.example.desofs.shared.dtos.CreateRefundRequest;
import com.example.desofs.shared.dtos.RefundRequestDTO;
import com.example.desofs.shared.mappers.IRefundMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RefundService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefundService Unit Tests")
class RefundServiceTest {

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private IRefundMapper refundMapper;

    @InjectMocks
    private RefundService refundService;

    private Order testOrder;
    private RefundRequest testRefund;

    private RefundRequestDTO testRefundDTO;
    private static final String AUTH0_ID = "auth0|user123";

    @BeforeEach
    void setUp() {
        testOrder = new Order(AUTH0_ID, "Receipt Name");
        testOrder.setId(1L);
        testOrder.setStatus(OrderStatus.COMPLETED);

        testRefund = new RefundRequest(testOrder, AUTH0_ID, new BigDecimal("15.00"), "Not satisfied");

        testRefundDTO = new RefundRequestDTO(
            100L,
            1L,
            AUTH0_ID,
            new BigDecimal("15.00"),
            RefundRequest.RefundStatus.REQUESTED.toString(),
            "Not satisfied",
            testRefund.getCreatedAt(),
            testRefund.getUpdatedAt()
        );
    }

    // Helper method to setup mapper for tests that need it
    private void setupRefundMapper() {
        when(refundMapper.toDTO(any(RefundRequest.class))).thenAnswer(inv -> {
            RefundRequest r = inv.getArgument(0);
            return new RefundRequestDTO(
                r.getId(),
                r.getOrder().getId(),
                r.getAuth0Id(),
                r.getAmount(),
                r.getStatus().toString(),
                r.getReason(),
                r.getCreatedAt(),
                r.getUpdatedAt()
            );
        });
    }

    // OPAQUE-BOX TESTS : Focus on external behavior (public API, outputs)
    @Nested
    @DisplayName("Opaque-box (behavior) tests")
    class OpaqueBoxTests {

        @Test
        @DisplayName("listAll() should return empty list when no refunds exist")
        void testListAll_WithEmptyRepository_ReturnsEmpty() {
            when(refundRequestRepository.findAll()).thenReturn(List.of());

            List<RefundRequestDTO> result = refundService.listAll();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("get(id) should return empty optional when refund not found")
        void testGet_WithInvalidId_ReturnsEmpty() {
            when(refundRequestRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<RefundRequestDTO> result = refundService.get(999L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("createRefundRequest() with null request should throw")
        void testCreateRefundRequest_NullRequest_Throws() {
            assertThatThrownBy(() -> refundService.createRefundRequest(AUTH0_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
        }

        @Test
        @DisplayName("createRefundRequest() with missing orderId should throw")
        void testCreateRefundRequest_MissingOrderId_Throws() {
            CreateRefundRequest request = new CreateRefundRequest();
            request.setAmount(new BigDecimal("15.00"));
            request.setReason("Reason");

            assertThatThrownBy(() -> refundService.createRefundRequest(AUTH0_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order ID");
        }

        @Test
        @DisplayName("createRefundRequest() with negative amount should throw")
        void testCreateRefundRequest_NegativeAmount_Throws() {
            CreateRefundRequest request = new CreateRefundRequest();
            request.setOrderId(1L);
            request.setAmount(new BigDecimal("-5.00"));
            request.setReason("Reason");

            assertThatThrownBy(() -> refundService.createRefundRequest(AUTH0_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("createRefundRequest() with zero amount should throw")
        void testCreateRefundRequest_ZeroAmount_Throws() {
            CreateRefundRequest request = new CreateRefundRequest();
            request.setOrderId(1L);
            request.setAmount(BigDecimal.ZERO);
            request.setReason("Reason");

            assertThatThrownBy(() -> refundService.createRefundRequest(AUTH0_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("createRefundRequest() with blank reason should throw")
        void testCreateRefundRequest_BlankReason_Throws() {
            CreateRefundRequest request = new CreateRefundRequest();
            request.setOrderId(1L);
            request.setAmount(new BigDecimal("15.00"));
            request.setReason("   ");

            assertThatThrownBy(() -> refundService.createRefundRequest(AUTH0_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");
        }

        @Test
        @DisplayName("createRefundRequest() with non-existent order should throw")
        void testCreateRefundRequest_OrderNotFound_Throws() {
            CreateRefundRequest request = new CreateRefundRequest();
            request.setOrderId(999L);
            request.setAmount(new BigDecimal("15.00"));
            request.setReason("Reason");

            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refundService.createRefundRequest(AUTH0_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("createRefundRequest() with different user should throw AccessDeniedException")
        void testCreateRefundRequest_DifferentUser_Throws() {
            CreateRefundRequest request = new CreateRefundRequest();
            request.setOrderId(1L);
            request.setAmount(new BigDecimal("15.00"));
            request.setReason("Reason");

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> refundService.createRefundRequest("auth0|different_user", request))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("createRefundRequest() with non-completed order should throw")
        void testCreateRefundRequest_NonCompletedOrder_Throws() {
            Order pendingOrder = new Order(AUTH0_ID, "Receipt");
            pendingOrder.setId(2L);
            pendingOrder.setStatus(OrderStatus.PENDING);

            CreateRefundRequest request = new CreateRefundRequest();
            request.setOrderId(2L);
            request.setAmount(new BigDecimal("15.00"));
            request.setReason("Reason");

            when(orderRepository.findById(2L)).thenReturn(Optional.of(pendingOrder));

            assertThatThrownBy(() -> refundService.createRefundRequest(AUTH0_ID, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("completed");
        }

        @Test
        @DisplayName("createRefundRequest() after 14-day deadline should throw")
        void testCreateRefundRequest_AfterDeadline_Throws() {
            Order oldOrder = new Order(AUTH0_ID, "Receipt");
            oldOrder.setId(2L);
            oldOrder.setStatus(OrderStatus.COMPLETED);
            // Simulate order created 15 days ago
            setCreatedAt(oldOrder, LocalDateTime.now().minusDays(15));

            CreateRefundRequest request = new CreateRefundRequest();
            request.setOrderId(2L);
            request.setAmount(new BigDecimal("15.00"));
            request.setReason("Reason");

            when(orderRepository.findById(2L)).thenReturn(Optional.of(oldOrder));

            assertThatThrownBy(() -> refundService.createRefundRequest(AUTH0_ID, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("approve(id) should set status to APPROVED")
        void testApprove_WithValidId_ApprovesRefund() {
            setupRefundMapper();
            RefundRequest approvedRefund = new RefundRequest(testOrder, AUTH0_ID, new BigDecimal("15.00"), "Reason");
            approvedRefund.setStatus(RefundRequest.RefundStatus.APPROVED);

            when(refundRequestRepository.findById(100L)).thenReturn(Optional.of(approvedRefund));
            when(refundRequestRepository.save(any(RefundRequest.class))).thenReturn(approvedRefund);

            RefundRequestDTO result = refundService.approve(100L);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("approve(id) with non-existent id should return null")
        void testApprove_WithInvalidId_ReturnsNull() {
            when(refundRequestRepository.findById(999L)).thenReturn(Optional.empty());

            RefundRequestDTO result = refundService.approve(999L);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("reject(id, reason) should set status to REJECTED")
        void testReject_WithValidId_RejectsRefund() {
            setupRefundMapper();
            RefundRequest rejectedRefund = new RefundRequest(testOrder, AUTH0_ID, new BigDecimal("15.00"), "Reason");
            rejectedRefund.setStatus(RefundRequest.RefundStatus.REJECTED);
            rejectedRefund.setReason("Duplicate order");

            when(refundRequestRepository.findById(100L)).thenReturn(Optional.of(rejectedRefund));
            when(refundRequestRepository.save(any(RefundRequest.class))).thenReturn(rejectedRefund);

            RefundRequestDTO result = refundService.reject(100L, "Duplicate order");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("reject(id, reason) with non-existent id should return null")
        void testReject_WithInvalidId_ReturnsNull() {
            when(refundRequestRepository.findById(999L)).thenReturn(Optional.empty());

            RefundRequestDTO result = refundService.reject(999L, "Reason");

            assertThat(result).isNull();
        }
    }

    // TRANSPARENT-BOX TESTS : inspect internal interactions and implementation details
    @Nested
    @DisplayName("Transparent-box (implementation) tests")
    class TransparentBoxTests {

        @Test
        @DisplayName("listAll() should call repository findAll exactly once")
        void testListAll_CallsRepositoryOnce() {
            when(refundRequestRepository.findAll()).thenReturn(List.of());

            refundService.listAll();

            verify(refundRequestRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("get() should call repository findById exactly once")
        void testGet_CallsRepositoryOnce() {
            when(refundRequestRepository.findById(100L)).thenReturn(Optional.of(testRefund));

            refundService.get(100L);

            verify(refundRequestRepository, times(1)).findById(100L);
        }

        @Test
        @DisplayName("createRefundRequest() should save exactly once")
        void testCreateRefundRequest_SavesRepositoryOnce() {
            CreateRefundRequest request = new CreateRefundRequest();
            request.setOrderId(1L);
            request.setAmount(new BigDecimal("15.00"));
            request.setReason("Reason");

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(refundRequestRepository.save(any(RefundRequest.class))).thenReturn(testRefund);

            refundService.createRefundRequest(AUTH0_ID, request);

            verify(refundRequestRepository, times(1)).save(any(RefundRequest.class));
        }

        @Test
        @DisplayName("approve() should save the refund exactly once")
        void testApprove_SavesRepositoryOnce() {
            when(refundRequestRepository.findById(100L)).thenReturn(Optional.of(testRefund));
            when(refundRequestRepository.save(any(RefundRequest.class))).thenReturn(testRefund);

            refundService.approve(100L);

            verify(refundRequestRepository, times(1)).save(any(RefundRequest.class));
        }

        @Test
        @DisplayName("reject() should save the refund exactly once")
        void testReject_SavesRepositoryOnce() {
            when(refundRequestRepository.findById(100L)).thenReturn(Optional.of(testRefund));
            when(refundRequestRepository.save(any(RefundRequest.class))).thenReturn(testRefund);

            refundService.reject(100L, "Reason");

            verify(refundRequestRepository, times(1)).save(any(RefundRequest.class));
        }
    }

    // Helper method to set private fields using reflection
    private void setCreatedAt(Object entity, LocalDateTime createdAt) {
        try {
            var field = entity.getClass().getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, createdAt);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}