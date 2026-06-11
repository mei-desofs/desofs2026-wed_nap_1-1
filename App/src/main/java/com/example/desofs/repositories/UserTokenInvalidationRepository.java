package com.example.desofs.repositories;

import com.example.desofs.domain.UserTokenInvalidation;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link UserTokenInvalidation} denylist entries.
 */
public interface UserTokenInvalidationRepository
        extends JpaRepository<UserTokenInvalidation, String> {
}
