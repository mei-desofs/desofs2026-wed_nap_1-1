package com.example.desofs.controllers;

import com.example.desofs.domain.Role;
import com.example.desofs.security.RoleGuard;
import com.example.desofs.shared.dtos.RoleRequestDTO;
import com.example.desofs.shared.dtos.UserDTO;
import com.example.desofs.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final RoleGuard roleGuard;

    public UserController(UserService userService, RoleGuard roleGuard) {
        this.userService = userService;
        this.roleGuard = roleGuard;
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getUsers(@AuthenticationPrincipal Jwt jwt) {
        roleGuard.requireRole(jwt, Role.ADMIN);
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/{id}/roles")
    public ResponseEntity<Void> assignRole(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestBody RoleRequestDTO dto) {
        roleGuard.requireRole(jwt, Role.ADMIN);
        userService.assignRole(jwt.getSubject(), id, dto.role());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/roles")
    public ResponseEntity<Void> removeRole(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestBody RoleRequestDTO dto) {
        roleGuard.requireRole(jwt, Role.ADMIN);
        userService.removeRole(jwt.getSubject(), id, dto.role());
        return ResponseEntity.noContent().build();
    }
}
