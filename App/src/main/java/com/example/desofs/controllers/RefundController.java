package com.example.desofs.controllers;

import com.example.desofs.shared.dtos.CreateRefundRequest;
import com.example.desofs.shared.dtos.RefundRequestDTO;
import com.example.desofs.services.RefundService;
import com.example.desofs.domain.RefundRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/refunds")
/**
 * REST controller for managing refund requests.
 * <p>
 * Exposes endpoints to list refund requests, create a new refund request,
 * and transition a request through approval, rejection and completion.
 * Delegates all business logic and data access to {@link RefundService}.
 */
/**
 * REST controller for managing refund requests.
 * <p>
 * Exposes endpoints to list refund requests, create a new refund request,
 * and transition a request through approval, rejection and completion.
 * Delegates all business logic and data access to {@link RefundService}.
 */
public class RefundController {

    /** Service handling refund business logic and data access. */
    private final RefundService refundService;

    /**
     * Constructs the controller with the required service.
     *
     * @param refundService service for refund operations
     */
    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    /**
     * Lists all refund requests.
     *
     * @return list of {@link RefundRequestDTO}
     */
    /**
     * Lists all refund requests.
     *
     * @return list of {@link RefundRequestDTO}
     */
    @GetMapping
    public List<RefundRequestDTO> list() {
        return refundService.listAll().stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * Retrieves a specific refund request by id.
     *
     * @param id refund request identifier
     * @return 200 OK with the request DTO when found, otherwise 404 Not Found
     */
    /**
     * Retrieves a specific refund request by id.
     *
     * @param id refund request identifier
     * @return 200 OK with the request DTO when found, otherwise 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<RefundRequestDTO> get(@PathVariable Long id) {
        RefundRequest refund = refundService.get(id).orElse(null);
        if (refund == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(refund));
    }

    /**
     * Creates a new refund request for a given order and user.
     * <p>
     * Validates that the referenced order and user exist; throws an exception
     * if validation fails.
     *
     * @param request payload containing orderId, userId, amount and reason
     * @return 201 Created with the created {@link RefundRequestDTO}
     * @throws IllegalArgumentException if order or user not found
     */
    /**
     * Creates a new refund request for a given order and user.
     * <p>
     * Validates that the referenced order and user exist; throws an exception
     * if validation fails.
     *
     * @param request payload containing orderId, userId, amount and reason
     * @return 201 Created with the created {@link RefundRequestDTO}
     * @throws IllegalArgumentException if order or user not found
     */
    @PostMapping
    public ResponseEntity<RefundRequestDTO> create(@RequestBody CreateRefundRequest request) {
        RefundRequest created = refundService.create(request);
        return ResponseEntity.created(URI.create("/api/refunds/" + created.getId())).body(toDTO(created));
    }

    /**
     * Approves the refund request identified by {@code id}.
     *
     * @param id refund request identifier
     * @return 200 OK with updated DTO when successful, otherwise 404 Not Found
     */
    /**
     * Approves the refund request identified by {@code id}.
     *
     * @param id refund request identifier
     * @return 200 OK with updated DTO when successful, otherwise 404 Not Found
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<RefundRequestDTO> approve(@PathVariable Long id) {
        RefundRequest refund = refundService.approve(id);
        if (refund == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(refund));
    }

    /**
     * Rejects a refund request with an optional reason.
     *
     * @param id refund request identifier
     * @param rejectReq payload containing the rejection reason
     * @return 200 OK with updated DTO when successful, otherwise 404 Not Found
     */
    /**
     * Rejects a refund request with an optional reason.
     *
     * @param id refund request identifier
     * @param rejectReq payload containing the rejection reason
     * @return 200 OK with updated DTO when successful, otherwise 404 Not Found
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<RefundRequestDTO> reject(@PathVariable Long id, @RequestBody RejectRequest rejectReq) {
        RefundRequest refund = refundService.reject(id, rejectReq.getReason());
        if (refund == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(refund));
    }

    /**
     * Marks the refund request as completed.
     *
     * @param id refund request identifier
     * @return 200 OK with updated DTO when successful, otherwise 404 Not Found
     */
    /**
     * Marks the refund request as completed.
     *
     * @param id refund request identifier
     * @return 200 OK with updated DTO when successful, otherwise 404 Not Found
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<RefundRequestDTO> complete(@PathVariable Long id) {
        RefundRequest refund = refundService.complete(id);
        if (refund == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(refund));
    }

    /**
     * Converts a domain {@link RefundRequest} to a transport {@link RefundRequestDTO}.
     *
     * @param refund domain refund request
     * @return corresponding DTO representation
     */
    /**
     * Converts a domain {@link RefundRequest} to a transport {@link RefundRequestDTO}.
     *
     * @param refund domain refund request
     * @return corresponding DTO representation
     */
    private RefundRequestDTO toDTO(RefundRequest refund) {
        return new RefundRequestDTO(
            refund.getId(),
            refund.getOrder().getId(),
            refund.getUserId(),
            refund.getAmount(),
            refund.getStatus().toString(),
            refund.getReason(),
            refund.getCreatedAt(),
            refund.getUpdatedAt()
        );
    }

    /**
     * Payload for rejection requests.
     */
    /**
     * Payload for rejection requests.
     */
    public static class RejectRequest {
        private String reason;

        /**
         * Gets the rejection reason.
         *
         * @return reason text
         */

        /**
         * Gets the rejection reason.
         *
         * @return reason text
         */
        public String getReason() { return reason; }

        /**
         * Sets the rejection reason.
         *
         * @param reason reason text
         */

        /**
         * Sets the rejection reason.
         *
         * @param reason reason text
         */
        public void setReason(String reason) { this.reason = reason; }
    }
}
