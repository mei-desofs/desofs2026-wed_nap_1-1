package com.example.desofs.shared.mappers;

import com.example.desofs.domain.Order;
import com.example.desofs.shared.dtos.OrderItemResponseDTO;
import com.example.desofs.shared.dtos.OrderResponseDTO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default implementation of {@link IOrderMapper} that converts
 * {@link Order} domain entities into {@link OrderResponseDTO} objects.
 */
@Component
public class OrderMapper implements IOrderMapper {

	/**
	 * Default constructor used by Spring.
	 */
	public OrderMapper() {
	}

	/**
	 * Maps an {@link Order} domain entity to its transport {@link OrderResponseDTO}.
	 *
	 * @param order domain order to convert
	 * @return transport {@link OrderResponseDTO} with order fields and item list
	 */
	@Override
	public OrderResponseDTO toResponseDTO(Order order) {
		List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
				.map(item -> new OrderItemResponseDTO(
						item.getMovie().getId(),
						item.getMovie().getTitle(),
						item.getQuantity(),
						item.getUnitPrice(),
						item.getSubtotal()))
				.toList();

		return new OrderResponseDTO(
				order.getId(),
				order.getStatus().name(),
				order.getReceiptName(),
				order.getTotalPrice(),
				order.getCreatedAt(),
				itemDTOs);
	}
}
