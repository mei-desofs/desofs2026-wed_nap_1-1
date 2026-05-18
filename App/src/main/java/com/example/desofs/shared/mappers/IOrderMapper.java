package com.example.desofs.shared.mappers;

import com.example.desofs.domain.Order;
import com.example.desofs.shared.dtos.OrderResponseDTO;

/**
 * Interface defining mapping operations for {@link Order} domain objects.
 */
public interface IOrderMapper {

    /**
     * Converts a domain {@link Order} to an {@link OrderResponseDTO}.
     *
     * @param order domain order
     * @return transport {@link OrderResponseDTO}
     */
    OrderResponseDTO toResponseDTO(Order order);
}
