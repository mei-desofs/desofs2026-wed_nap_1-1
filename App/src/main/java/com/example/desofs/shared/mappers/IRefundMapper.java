package com.example.desofs.shared.mappers;

import com.example.desofs.domain.RefundRequest;
import com.example.desofs.shared.dtos.RefundRequestDTO;

/**
 * Mapper interface responsible for converting {@link RefundRequest}
 * domain objects to {@link RefundRequestDTO} transport objects.
 */
public interface IRefundMapper {

    /**
     * Converts a {@link RefundRequest} domain entity into its DTO form.
     *
     * @param refund domain refund request
     * @return transport {@link RefundRequestDTO}
     */
    RefundRequestDTO toDTO(RefundRequest refund);
}
