package com.emovieshop.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

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
