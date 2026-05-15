package com.example.desofs.shared.dtos;

import com.example.desofs.domain.Role;

/**
 * Request payload used to assign or remove a role.
 *
 * @param role role to be applied
 */
public record RoleRequestDTO(Role role) {}