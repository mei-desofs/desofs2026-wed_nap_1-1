package com.example.desofs.services;

import com.example.desofs.shared.dtos.OrderResponseDTO;
import com.example.desofs.shared.dtos.PurchaseRequestDTO;

/**
 * Interface for order-related business logic.
 */
public interface IOrderService {
    OrderResponseDTO createOrder(String auth0Id, PurchaseRequestDTO request);
}
