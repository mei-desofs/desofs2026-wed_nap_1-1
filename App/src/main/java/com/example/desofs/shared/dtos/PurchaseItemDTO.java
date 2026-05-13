package com.example.desofs.shared.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload representing a single movie item in a purchase order.
 *
 * <p>Validates that the movie identifier is present and that the quantity is
 * at least one.</p>
 *
 * @param movieId movie identifier
 * @param quantity requested quantity
 */
public record PurchaseItemDTO(
        @NotNull(message = "Movie ID is required")
        Long movieId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {
}
