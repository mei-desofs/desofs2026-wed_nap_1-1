package com.example.desofs.shared.mappers;

import com.example.desofs.domain.RefundRequest;
import com.example.desofs.shared.dtos.RefundRequestDTO;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link IRefundMapper} converting refund
 * domain objects into transport DTOs.
 */
@Component
public final class RefundMapper implements IRefundMapper {

    /**
     * Default constructor for DI container instantiation.
     */
    public RefundMapper() {
    }

    /**
     * Converts a {@link RefundRequest} domain entity into a {@link RefundRequestDTO}.
     *
     * @param refund domain refund request
     * @return transport {@link RefundRequestDTO} representing the refund
     */
    @Override
    public RefundRequestDTO toDTO(RefundRequest refund) {
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
