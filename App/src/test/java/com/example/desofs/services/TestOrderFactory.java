package com.example.desofs.services;

import com.example.desofs.domain.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;

/**
 * Factory for creating test Order instances with reflection-set IDs.
 */
class TestOrderFactory {

    static Order createTestOrder(Long orderId, String receiptName, int itemCount) {
        Order order = new Order("auth0|test123", receiptName);
        setId(order, orderId);

        for (int i = 1; i <= itemCount; i++) {
            Movie movie = new Movie("Test Movie " + i, "Random Description", "Youtube", "Action", new BigDecimal("9.99"), 10);
            setId(movie, (long) i);

            OrderItem item = new OrderItem(movie, 1, movie.getPrice());
            order.addItem(item);
        }

        return order;
    }

    private static void setId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set ID via reflection", e);
        }
    }
}

