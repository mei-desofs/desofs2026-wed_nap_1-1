package com.example.desofs.security;

import com.example.desofs.domain.Role;
import com.example.desofs.shared.dtos.UserDTO;

import java.util.List;

/**
 * Thin abstraction over the subset of Auth0 Management API operations
 * required by the user/role administration use case (UC8).
 */
public interface IAuth0ManagementClient {

    /**
     * Lists users known to the Auth0 tenant (first page, default size).
     *
     * @return list of user DTOs
     */
    List<UserDTO> listUsers();

    /**
     * Assigns a role to the given user via {@code POST /api/v2/users/{id}/roles}.
     *
     * @param userId Auth0 user identifier (e.g. {@code auth0|...})
     * @param role application role to assign
     */
    void assignRole(String userId, Role role);

    /**
     * Removes a role from the given user via
     * {@code DELETE /api/v2/users/{id}/roles}.
     *
     * @param userId Auth0 user identifier
     * @param role application role to remove
     */
    void removeRole(String userId, Role role);

    /**
     * Invalidates all active SSO sessions for the given user via
     * {@code DELETE /api/v2/users/{id}/sessions}.
     *
     * <p>This forces the user's SPA / browser to perform a fresh interactive
     * login on the next silent-auth attempt, ensuring that any subsequent
     * access token reflects the latest role assignments. It does <em>not</em>
     * revoke access tokens that have already been issued; those are rejected
     * by the server-side denylist enforced in {@code TokenFreshnessFilter}.</p>
     *
     * <p>Failures are swallowed by the implementation (logged at WARN level)
     * to avoid coupling administrative operations to Auth0 availability;
     * the denylist alone is sufficient to deny access from stale tokens.</p>
     *
     * @param userId Auth0 user identifier
     */
    void invalidateSessions(String userId);
}
