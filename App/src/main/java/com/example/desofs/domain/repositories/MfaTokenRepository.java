package com.example.desofs.domain.repositories;

import com.example.desofs.domain.entities.MfaToken;
import com.example.desofs.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MfaTokenRepository extends JpaRepository<MfaToken, Long> {
    Optional<MfaToken> findByUser(User user);
}
