package com.example.desofs.shared.mappers;

import com.example.desofs.domain.RefundRequest;
import com.example.desofs.shared.dtos.RefundRequestDTO;

/**
 * Maps refund domain objects to transport DTOs.
 */
public final class RefundMapper {

    private RefundMapper() {
    }

    /**
     * Converts a domain {@link RefundRequest} to a transport {@link RefundRequestDTO}.
     *
     * @param refund domain refund request
     * @return corresponding DTO representation
     */
    public static RefundRequestDTO toDTO(RefundRequest refund) {
        return new RefundRequestDTO(
            refund.getId(),
            refund.getOrder().getId(),
            refund.getAuth0Id(),
            refund.getAmount(),
            refund.getStatus().toString(),
            refund.getReason(),
            refund.getCreatedAt(),
            refund.getUpdatedAt()
        );
    }
}
