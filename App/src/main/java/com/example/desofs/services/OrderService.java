package com.example.desofs.services;

import com.emovieshop.domain.model.*;
import com.emovieshop.repositories.MovieRepository;
import com.emovieshop.repositories.OrderRepository;
import com.emovieshop.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private static final int MAX_ITEMS_PER_ORDER = 10;

    private final OrderRepository orderRepository;
    private final MovieRepository movieRepository;
    private final ReceiptFileService receiptFileService;

    public OrderService(OrderRepository orderRepository,
                        MovieRepository movieRepository,
                        ReceiptFileService receiptFileService) {
        this.orderRepository = orderRepository;
        this.movieRepository = movieRepository;
        this.receiptFileService = receiptFileService;
    }

    @Transactional
    public OrderResponseDTO createOrder(String auth0Id, PurchaseRequestDTO request) {
        logger.info("Processing purchase request for user: {}", auth0Id);

        // Validate item count (defense-in-depth, DTO validation also checks this)
        if (request.items().size() > MAX_ITEMS_PER_ORDER) {
            throw new IllegalArgumentException("Cannot purchase more than " + MAX_ITEMS_PER_ORDER + " movies in a single order");
        }

        // Validate no duplicate movie IDs in request
        validateNoDuplicateMovies(request.items());

        // Sanitize receipt name early to fail fast
        String sanitizedReceiptName = receiptFileService.sanitizeReceiptName(request.receiptName());

        // Create order with auth0Id directly
        Order order = new Order(auth0Id, sanitizedReceiptName);

        // Process each item: validate movie exists, check stock, resolve price from DB
        for (PurchaseItemDTO itemDTO : request.items()) {
            Movie movie = movieRepository.findById(itemDTO.movieId())
                    .orElseThrow(() -> {
                        logger.warn("Purchase references non-existent movie ID: {}", itemDTO.movieId());
                        return new IllegalArgumentException("Movie not found: " + itemDTO.movieId());
                    });

            // Decrease stock (throws if insufficient)
            movie.decreaseStock(itemDTO.quantity());

            // Price is always resolved from the database, never from the client
            OrderItem orderItem = new OrderItem(movie, itemDTO.quantity(), movie.getPrice());
            order.addItem(orderItem);
        }

        // Persist order (atomic transaction includes stock update)
        Order savedOrder = orderRepository.save(order);

        // Create receipt file (OS operation)
        try {
            receiptFileService.createReceiptFile(savedOrder);
        } catch (IOException e) {
            logger.error("Failed to create receipt file for order {}: {}", savedOrder.getId(), e.getMessage());
            // Receipt file creation failure should not rollback the order
            // The order is still valid, the receipt can be regenerated
        } catch (SecurityException e) {
            logger.error("Security violation during receipt creation for order {}: {}", savedOrder.getId(), e.getMessage());
        }

        logger.info("Order {} created successfully for user {}", savedOrder.getId(), auth0Id);

        return toResponseDTO(savedOrder);
    }

    private void validateNoDuplicateMovies(List<PurchaseItemDTO> items) {
        Map<Long, Integer> movieIdCounts = new HashMap<>();
        for (PurchaseItemDTO item : items) {
            movieIdCounts.merge(item.movieId(), 1, Integer::sum);
        }
        boolean hasDuplicates = movieIdCounts.values().stream().anyMatch(count -> count > 1);
        if (hasDuplicates) {
            throw new IllegalArgumentException("Duplicate movie IDs are not allowed in a single order");
        }
    }

    private OrderResponseDTO toResponseDTO(Order order) {
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
