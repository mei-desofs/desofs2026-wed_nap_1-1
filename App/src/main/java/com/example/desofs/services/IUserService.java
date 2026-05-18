package com.example.desofs.services;

import com.example.desofs.domain.Role;
import com.example.desofs.shared.dtos.UserDTO;

import java.util.List;

/**
 * Interface for user administration operations.
 */
public interface IUserService {
    List<UserDTO> getAllUsers();

    void assignRole(String actorId, String targetUserId, Role role);

    void removeRole(String actorId, String targetUserId, Role role);
}
