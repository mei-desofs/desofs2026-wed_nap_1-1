package com.example.desofs.controllers;

import com.example.desofs.domain.Role;
import com.example.desofs.shared.dtos.OrderResponseDTO;
import com.example.desofs.shared.dtos.PurchaseRequestDTO;
import com.example.desofs.security.RoleGuard;
import com.example.desofs.services.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final RoleGuard roleGuard;

    public OrderController(OrderService orderService, RoleGuard roleGuard) {
        this.orderService = orderService;
        this.roleGuard = roleGuard;
    }

    /**
     * POST /api/orders
     * Creates a new purchase order. Only accessible by users with the CUSTOMER role.
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
