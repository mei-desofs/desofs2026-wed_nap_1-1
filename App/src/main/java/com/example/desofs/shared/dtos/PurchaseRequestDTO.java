package com.example.desofs.shared.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request payload used to create a purchase order.
 *
 * <p>Contains the receipt name and the list of purchased movie items.
 * Validation ensures the request is not empty and stays within the supported
 * limits.</p>
 *
 * @param receiptName name to display on the receipt
 * @param items list of purchased items
 */
public record PurchaseRequestDTO(
        @NotBlank(message = "Receipt name is required")
        @Size(max = 100, message = "Receipt name must not exceed 100 characters")
        String receiptName,

        @NotEmpty(message = "At least one item is required")
        @Size(max = 10, message = "Cannot purchase more than 10 movies in a single order")
        @Valid
        List<PurchaseItemDTO> items
) {
}
