package com.example.desofs.application.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDTO(
        Long orderId,
        String status,
        String receiptName,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        List<OrderItemResponseDTO> items
) {
}
