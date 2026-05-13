package com.example.desofs.repositories;

import com.example.desofs.domain.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisting and retrieving {@link RefundRequest} entities.
 */
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
}
