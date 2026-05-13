package com.example.desofs.repositories;

import com.example.desofs.domain.MfaToken;
import com.example.desofs.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MfaTokenRepository extends JpaRepository<MfaToken, Long> {
    Optional<MfaToken> findByUser(User user);
}
