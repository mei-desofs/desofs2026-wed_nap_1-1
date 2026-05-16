package com.example.desofs.security;

import com.example.desofs.domain.Role;
import org.springframework.security.oauth2.jwt.Jwt;

/** Interface for role-based guard used across controllers/services. */
public interface IRoleGuard {
    void requireRole(Jwt jwt, Role role);
}
