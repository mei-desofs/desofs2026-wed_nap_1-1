package com.example.desofs.services;

import com.example.desofs.domain.Order;
import com.example.desofs.domain.OrderStatus;
import com.example.desofs.domain.RefundRequest;
import com.example.desofs.repositories.OrderRepository;
import com.example.desofs.repositories.RefundRequestRepository;
import com.example.desofs.shared.dtos.CreateRefundRequest;
import com.example.desofs.shared.dtos.RefundRequestDTO;
import com.example.desofs.shared.mappers.RefundMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service responsible for managing refund requests.
 * <p>
 * Provides operations to list, retrieve, create and transition refund
 * requests. The service performs validation of referenced domain objects
 * (orders and users) when creating new refund requests.
 */
@Service
public class RefundService {
    /** Logger for audit and error tracking. */
    private static final Logger logger = LoggerFactory.getLogger(RefundService.class);

    /** Repository for refund request persistence. */
    private final RefundRequestRepository refundRequestRepository;

    /** Repository used to lookup orders referenced by refund requests. */
    private final OrderRepository orderRepository;

    /**
     * Constructs the service with required repositories.
     *
     * @param refundRequestRepository repository for refund requests
     * @param orderRepository repository for orders
     */
    public RefundService(RefundRequestRepository refundRequestRepository, OrderRepository orderRepository) {
        this.refundRequestRepository = refundRequestRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Returns all refund requests.
     *
     * @return list of refund requests
     */
    public List<RefundRequestDTO> listAll() {
        return refundRequestRepository.findAll().stream()
            .map(RefundMapper::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a refund request by id.
     *
     * @param id refund request identifier
     * @return optional refund request
     */
    public Optional<RefundRequestDTO> get(Long id) {
        return refundRequestRepository.findById(id).map(RefundMapper::toDTO);
    }

    /**
     * Creates a refund request from the provided request DTO.
     * Validates that the referenced order exists and binds the request to the
     * authenticated user identity.
     *
     * @param auth0Id authenticated user subject from the JWT
     * @param request refund creation request containing orderId, amount, and reason
     * @return created RefundRequest
     * @throws IllegalArgumentException if the order is not found
     */
    @Transactional
    public RefundRequestDTO createRefundRequest(String auth0Id, CreateRefundRequest request) {
        validateCreateRequest(request);

        logger.info("Creating refund request for orderId: {}, amount: {}, reason: {}",
            request.getOrderId(), request.getAmount(), request.getReason());

        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.getOrderId()));

        if (!auth0Id.equals(order.getAuth0Id())) {
            throw new AccessDeniedException("Cannot request a refund for another customer's order");
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Refunds can only be requested for completed orders");
        }

        LocalDateTime refundDeadline = order.getCreatedAt().plusDays(14);
        if (LocalDateTime.now().isAfter(refundDeadline)) {
            throw new IllegalStateException("Refund request period has expired");
        }

        RefundRequest refund = new RefundRequest(order, auth0Id, request.getAmount(), request.getReason().trim());
    
        RefundRequest savedRefund = refundRequestRepository.save(refund);
        logger.info("Refund request created with ID: {}", savedRefund.getId());

        return RefundMapper.toDTO(savedRefund);
    }

    /**
     * Validates the incoming refund creation request before any persistence or
     * authorization checks are performed.
     *
     * @param request refund creation payload
     * @throws IllegalArgumentException if the payload is missing or contains
     *         invalid fields
     */
    private void validateCreateRequest(CreateRefundRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Refund request payload is required");
        }
        if (request.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new IllegalArgumentException("Refund reason is required");
        }
        if (request.getReason().length() > RefundRequest.REASON_MAX_LENGTH) {
            throw new IllegalArgumentException("Refund reason must not exceed " + RefundRequest.REASON_MAX_LENGTH + " characters");
        }
    }

    /**
     * Approves the refund request with the given id.
     *
     * @param id refund request identifier
     * @return updated refund request or null if not found
     */
    public RefundRequestDTO approve(Long id) {
        return refundRequestRepository.findById(id).map(refund -> {
            refund.setStatus(RefundRequest.RefundStatus.APPROVED);
            return RefundMapper.toDTO(refundRequestRepository.save(refund));
        }).orElse(null);
    }

    /**
     * Rejects the refund request with the given id and records a reason.
     *
     * @param id refund request identifier
     * @param reason rejection reason
     * @return updated refund request or null if not found
     */
    public RefundRequestDTO reject(Long id, String reason) {
        return refundRequestRepository.findById(id).map(refund -> {
            refund.setStatus(RefundRequest.RefundStatus.REJECTED);
            refund.setReason(reason);
            return RefundMapper.toDTO(refundRequestRepository.save(refund));
        }).orElse(null);
    }
}
