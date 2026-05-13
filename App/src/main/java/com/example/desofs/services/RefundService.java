package com.example.desofs.services;

import com.example.desofs.domain.Order;
import com.example.desofs.domain.RefundRequest;
import com.example.desofs.domain.User;
import com.example.desofs.repositories.OrderRepository;
import com.example.desofs.repositories.RefundRequestRepository;
import com.example.desofs.repositories.UserRepository;
import com.example.desofs.shared.dtos.CreateRefundRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RefundService {
    private final RefundRequestRepository refundRequestRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public RefundService(RefundRequestRepository refundRequestRepository,
                        OrderRepository orderRepository,
                        UserRepository userRepository) {
        this.refundRequestRepository = refundRequestRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    public List<RefundRequest> listAll() {
        return refundRequestRepository.findAll();
    }

    public Optional<RefundRequest> get(Long id) {
        return refundRequestRepository.findById(id);
    }

    /**
     * Creates a refund request from the provided request DTO.
     * Validates that the referenced order and user exist.
     *
     * @param request refund creation request containing orderId, userId, amount, and reason
     * @return created RefundRequest
     * @throws IllegalArgumentException if order or user not found
     */
    public RefundRequest create(CreateRefundRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.getOrderId()));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));

        RefundRequest refund = new RefundRequest();
        refund.setOrder(order);
        refund.setUser(user);
        refund.setAmount(request.getAmount());
        refund.setReason(request.getReason());

        return refundRequestRepository.save(refund);
    }

    public RefundRequest approve(Long id) {
        return refundRequestRepository.findById(id).map(refund -> {
            refund.setStatus(RefundRequest.RefundStatus.APPROVED);
            return refundRequestRepository.save(refund);
        }).orElse(null);
    }

    public RefundRequest reject(Long id, String reason) {
        return refundRequestRepository.findById(id).map(refund -> {
            refund.setStatus(RefundRequest.RefundStatus.REJECTED);
            refund.setReason(reason);
            return refundRequestRepository.save(refund);
        }).orElse(null);
    }

    public RefundRequest complete(Long id) {
        return refundRequestRepository.findById(id).map(refund -> {
            //TEM ERRO AQUIIIIIII!
            //refund.setStatus(RefundRequest.RefundStatus.COMPLETED);
            return refundRequestRepository.save(refund);
        }).orElse(null);
    }
}
