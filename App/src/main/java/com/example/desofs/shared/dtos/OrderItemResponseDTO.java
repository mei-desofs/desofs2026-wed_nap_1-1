package com.example.desofs.shared.dtos;

import java.math.BigDecimal;

/**
 * Response payload representing a single order item.
 *
 * <p>Exposes the movie details, quantity, pricing information, and subtotal
 * for client consumption.</p>
 *
 * @param movieId movie identifier
 * @param movieTitle movie title
 * @param quantity ordered quantity
 * @param unitPrice unit price
 * @param subtotal computed subtotal
 */
public record OrderItemResponseDTO(
        Long movieId,
        String movieTitle,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
