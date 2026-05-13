package com.example.desofs.domain.repositories;

import com.example.desofs.domain.entities.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
}
