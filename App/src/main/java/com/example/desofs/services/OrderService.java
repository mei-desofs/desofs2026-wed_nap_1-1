package com.example.desofs.services;

import com.example.desofs.domain.*;
import com.example.desofs.repositories.MovieRepository;
import com.example.desofs.repositories.OrderRepository;
import com.example.desofs.shared.dtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing movie purchase orders.
 * <p>
 * Handles order creation with full validation: item limits, duplicate detection,
 * stock availability, and price resolution from database. Manages receipt file
 * creation and transaction integrity. Non-blocking receipt generation failures do
 * not rollback the persisted order.
 */
@Service
public class OrderService {

    /** Logger for audit and error tracking. */
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    /** Maximum number of distinct items allowed per order. */
    private static final int MAX_ITEMS_PER_ORDER = 10;

    /** Repository for order persistence. */
    private final OrderRepository orderRepository;

    /** Repository for movie lookup and stock management. */
    private final MovieRepository movieRepository;

    /** Service responsible for receipt file generation. */
    private final ReceiptFileService receiptFileService;

    /**
     * Constructs the service with required dependencies.
     *
     * @param orderRepository repository for order persistence
     * @param movieRepository repository for movie lookups
     * @param receiptFileService service for receipt file operations
     */
    public OrderService(OrderRepository orderRepository,
                        MovieRepository movieRepository,
                        ReceiptFileService receiptFileService) {
        this.orderRepository = orderRepository;
        this.movieRepository = movieRepository;
        this.receiptFileService = receiptFileService;
    }

    /**
     * Creates and persists a new order for the given user.
     * <p>
     * Performs comprehensive validation: item count limits, duplicate movie detection,
     * movie existence, and stock availability. Prices are resolved from the database,
     * never accepted from the client. Receipt file creation is non-blocking and
     * failures do not rollback the transaction.
     *
     * @param auth0Id user identifier from Auth0
     * @param request purchase request containing items and receipt name
     * @return {@link OrderResponseDTO} containing the created order details
     * @throws IllegalArgumentException if item count exceeds limit, duplicate movies
     *         detected, or movie not found
     */
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

    /**
     * Validates that no movie appears more than once in the purchase items.
     *
     * @param items list of purchase items to validate
     * @throws IllegalArgumentException if any movie ID appears multiple times
     */
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

    /**
     * Converts a domain {@link Order} to a transport {@link OrderResponseDTO}.
     *
     * @param order domain order object
     * @return corresponding DTO with order details and item breakdown
     */
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
