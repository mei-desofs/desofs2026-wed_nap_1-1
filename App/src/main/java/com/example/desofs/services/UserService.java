package com.example.desofs.services;

import com.example.desofs.domain.AuditLog;
import com.example.desofs.domain.Role;
import com.example.desofs.repositories.AuditLogRepository;
import com.example.desofs.shared.dtos.UserDTO;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class UserService {
    // private final Auth0ManagementClient auth0Client;
    private final AuditLogRepository auditLogRepository;

    public UserService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

//    public List<UserDTO> getAllUsers() {
//        return auth0Client.getUsers();
//    }
//
//    public void assignRole(String actorId, String targetUserId, Role role) {
//        auth0Client.assignRoleToUser(targetUserId, role);
//        auditLogRepository.save(AuditLog.of(actorId, targetUserId, role, "ASSIGN"));
//    }
//
//    public void removeRole(String actorId, String targetUserId, Role role) {
//        auth0Client.removeRoleFromUser(targetUserId, role);
//        auditLogRepository.save(AuditLog.of(actorId, targetUserId, role, "REMOVE"));
//    }
}
