package com.example.desofs.application.dtos;

import java.math.BigDecimal;

public record OrderItemResponseDTO(
        Long movieId,
        String movieTitle,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
