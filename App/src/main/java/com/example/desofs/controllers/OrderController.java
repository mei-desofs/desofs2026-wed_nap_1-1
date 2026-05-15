package com.example.desofs.controllers;

import com.example.desofs.domain.Role;
import com.example.desofs.shared.dtos.OrderResponseDTO;
import com.example.desofs.shared.dtos.PurchaseRequestDTO;
import com.example.desofs.security.IRoleGuard;
import com.example.desofs.services.IOrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for order creation endpoints.
 * <p>
 * Exposes purchase order creation and enforces customer access through JWT
 * role checks.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    /** Logger for request tracing and diagnostics. */
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    /** Service responsible for order processing and persistence. */
    private final IOrderService orderService;

    /** Guard that enforces role-based access checks. */
    private final IRoleGuard roleGuard;

    /**
     * Constructs the controller with required dependencies.
     *
     * @param orderService service to handle order business logic
     * @param roleGuard component used to enforce role checks
     */
    public OrderController(IOrderService orderService, IRoleGuard roleGuard) {
        this.orderService = orderService;
        this.roleGuard = roleGuard;
    }

    /**
     * Creates a new purchase order.
     * <p>
     * Only users with the {@link Role#CUSTOMER} role can call this endpoint.
     * @param jwt authenticated JWT principal
     * @param request purchase request payload
     * @return HTTP 201 with the created order response
     */
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PurchaseRequestDTO request) {

        String auth0Id = jwt.getSubject();
        logger.info("Purchase request received from user: {}", auth0Id);

        // Enforce CUSTOMER role via RoleGuard (reads role from JWT claims)
        roleGuard.requireRole(jwt, Role.CUSTOMER);

        OrderResponseDTO response = orderService.createOrder(auth0Id, request);

        logger.info("Purchase completed. Order ID: {}", response.orderId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
