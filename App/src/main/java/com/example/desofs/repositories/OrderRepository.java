package com.example.desofs.repositories;

import com.example.desofs.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisting and retrieving {@link Order} entities.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
}
