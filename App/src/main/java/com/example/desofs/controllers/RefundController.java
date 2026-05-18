package com.example.desofs.controllers;

import com.example.desofs.shared.dtos.CreateRefundRequest;
import com.example.desofs.shared.dtos.RejectRefundRequest;
import com.example.desofs.shared.dtos.RefundRequestDTO;
import com.example.desofs.services.IRefundService;
import com.example.desofs.services.IAuditLogService;
import com.example.desofs.domain.Role;
import com.example.desofs.security.IRoleGuard;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST controller for managing refund requests.
 * <p>
 * Exposes endpoints to list refund requests, create a new refund request,
 * and transition a request through approval, rejection and completion.
 * Delegates all business logic and data access to {@link IRefundService}.
 */
@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    /** Logger for request tracing. */
    private static final Logger logger = LoggerFactory.getLogger(RefundController.class);

    /** Service handling refund business logic and data access. */
    private final IRefundService refundService;

    /** Service responsible for recording audit log entries. */
    private final IAuditLogService auditLogService;

    /** Guard that enforces role-based access checks. */
    private final IRoleGuard roleGuard;

    /**
     * Constructs the controller with the required service and role guard.
     *
     * @param refundService service for refund operations
     * @param roleGuard component to enforce role checks
     */
    public RefundController(IRefundService refundService, IAuditLogService auditLogService, IRoleGuard roleGuard) {
        this.refundService = refundService;
        this.auditLogService = auditLogService;
        this.roleGuard = roleGuard;
    }

    /**
     * Lists all refund requests.
     *
     * @return list of {@link RefundRequestDTO}
     */
    @GetMapping
    public List<RefundRequestDTO> list() {
        return refundService.listAll();
    }

    /**
     * Retrieves a specific refund request by id.
     *
     * @param id refund request identifier
     * @return 200 OK with the request DTO when found, otherwise 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<RefundRequestDTO> get(@PathVariable Long id) {
        RefundRequestDTO refund = refundService.get(id).orElse(null);
        if (refund == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(refund);
    }

    /**
     * Creates a new refund request for a given order and user.
     * <p>
     * Only users with the {@link Role#CUSTOMER} role can call this endpoint.
     * @param jwt authenticated JWT principal
     * @param request purchase request payload
     * @return HTTP 201 with the created refund request response
     */
    @PostMapping
    public ResponseEntity<RefundRequestDTO> createRefundRequest(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRefundRequest request) {

        String auth0Id = jwt.getSubject();
        logger.info("Refund request received from user: {}", auth0Id);

        // Enforce CUSTOMER role via RoleGuard (reads role from JWT claims)
        roleGuard.requireRole(jwt, Role.CUSTOMER);

        RefundRequestDTO created = refundService.createRefundRequest(auth0Id, request);
        auditLogService.log(auth0Id, created.getUserId(), Role.CUSTOMER, "CREATE_REFUND_REQUEST");

        logger.info("Refund request created. Refund ID: {}", created.getId());
        return ResponseEntity.created(URI.create("/api/refunds/" + created.getId())).body(created);
    }

    /**
     * Approves the refund request identified by {@code id}.
     * Enforces that the authenticated user has the {@link Role#SUPPORT} role.
     * @param id refund request identifier
     * @param jwt authenticated JWT principal
     * @return 200 OK with updated DTO when successful, otherwise 404 Not Found
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<RefundRequestDTO> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {

        String auth0Id = jwt.getSubject();
        logger.info("Refund request analyzed by user: {}", auth0Id);

        // Enforce SUPPORT role via RoleGuard (reads role from JWT claims)
        roleGuard.requireRole(jwt, Role.SUPPORT);

        RefundRequestDTO refund = refundService.approve(id);
        if (refund == null) return ResponseEntity.notFound().build();
        auditLogService.log(jwt.getSubject(), refund.getUserId(), Role.SUPPORT, "APPROVE_REFUND");
        logger.info("Refund request approved. Refund ID: {}", refund.getId());
        return ResponseEntity.ok(refund);
    }

    /**
     * Rejects a refund request with an optional reason.
     * Enforces that the authenticated user has the {@link Role#SUPPORT} role.
     * @param jwt authenticated JWT principal
     * @param id refund request identifier
     * @param rejectReq payload containing the rejection reason
     * @return 200 OK with updated DTO when successful, otherwise 404 Not Found
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<RefundRequestDTO> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @RequestBody RejectRefundRequest rejectReq) {
        String auth0Id = jwt.getSubject();
        logger.info("Refund request analyzed by user: {}", auth0Id);

        // Enforce SUPPORT role via RoleGuard (reads role from JWT claims)
        roleGuard.requireRole(jwt, Role.SUPPORT);

        RefundRequestDTO refund = refundService.reject(id, rejectReq.getReason());
        if (refund == null) return ResponseEntity.notFound().build();
        auditLogService.log(jwt.getSubject(), refund.getUserId(), Role.SUPPORT, "REJECT_REFUND");
        logger.info("Refund request rejected. Refund ID: {}", refund.getId());
        return ResponseEntity.ok(refund);
    }   
}
