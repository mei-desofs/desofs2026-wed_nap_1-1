package com.example.desofs.controllers;

import com.example.desofs.application.dtos.CreateRefundRequest;
import com.example.desofs.application.dtos.RefundRequestDTO;
import com.example.desofs.application.services.RefundService;
import com.example.desofs.domain.entities.Order;
import com.example.desofs.domain.entities.RefundRequest;
import com.example.desofs.domain.entities.User;
import com.example.desofs.domain.repositories.OrderRepository;
import com.example.desofs.domain.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {
    private final RefundService refundService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public RefundController(RefundService refundService, OrderRepository orderRepository, UserRepository userRepository) {
        this.refundService = refundService;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<RefundRequestDTO> list() {
        return refundService.listAll().stream()
            .map(this::toDTO)
            .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RefundRequestDTO> get(@PathVariable Long id) {
        RefundRequest refund = refundService.get(id).orElse(null);
        if (refund == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(refund));
    }

    @PostMapping
    public ResponseEntity<RefundRequestDTO> create(@RequestBody CreateRefundRequest request) {
        Order order = orderRepository.findById(request.getOrderId()).orElse(null);
        User user = userRepository.findById(request.getUserId()).orElse(null);

        if (order == null || user == null) {
            return ResponseEntity.badRequest().build();
        }

        RefundRequest refund = new RefundRequest();
        refund.setOrder(order);
        refund.setUser(user);
        refund.setAmount(request.getAmount());
        refund.setReason(request.getReason());

        RefundRequest created = refundService.create(refund);
        return ResponseEntity.created(URI.create("/api/refunds/" + created.getId())).body(toDTO(created));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<RefundRequestDTO> approve(@PathVariable Long id) {
        RefundRequest refund = refundService.approve(id);
        if (refund == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(refund));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<RefundRequestDTO> reject(@PathVariable Long id, @RequestBody RejectRequest rejectReq) {
        RefundRequest refund = refundService.reject(id, rejectReq.getReason());
        if (refund == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(refund));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<RefundRequestDTO> complete(@PathVariable Long id) {
        RefundRequest refund = refundService.complete(id);
        if (refund == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(refund));
    }

    private RefundRequestDTO toDTO(RefundRequest refund) {
        return new RefundRequestDTO(
            refund.getId(),
            refund.getOrder().getId(),
            refund.getUser().getId(),
            refund.getAmount(),
            refund.getStatus().toString(),
            refund.getReason(),
            refund.getCreatedAt(),
            refund.getUpdatedAt()
        );
    }

    public static class RejectRequest {
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
