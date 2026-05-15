package com.example.desofs.shared.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload representing a complete order.
 *
 * <p>Includes the order identifier, lifecycle status, receipt name, total
 * price, creation timestamp, and the list of order items.</p>
 *
 * @param orderId order identifier
 * @param status order status
 * @param receiptName receipt display name
 * @param totalPrice total order price
 * @param createdAt order creation timestamp
 * @param items list of order item responses
 */
public record OrderResponseDTO(
        Long orderId,
        String status,
        String receiptName,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        List<OrderItemResponseDTO> items
) {
    public OrderResponseDTO {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
