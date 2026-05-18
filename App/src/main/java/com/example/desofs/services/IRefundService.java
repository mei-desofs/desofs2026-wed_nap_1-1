package com.example.desofs.services;

import com.example.desofs.shared.dtos.CreateRefundRequest;
import com.example.desofs.shared.dtos.RefundRequestDTO;

import java.util.List;
import java.util.Optional;

/**
 * Interface for refund request operations.
 */
public interface IRefundService {
    List<RefundRequestDTO> listAll();

    Optional<RefundRequestDTO> get(Long id);

    RefundRequestDTO createRefundRequest(String auth0Id, CreateRefundRequest request);

    RefundRequestDTO approve(Long id);

    RefundRequestDTO reject(Long id, String reason);
}
