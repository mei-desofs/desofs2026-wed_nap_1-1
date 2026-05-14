package com.example.desofs.controllers;

import com.example.desofs.domain.Role;
import com.example.desofs.security.RoleGuard;
import com.example.desofs.shared.dtos.RoleRequestDTO;
import com.example.desofs.shared.dtos.UserDTO;
import com.example.desofs.services.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
/**
 * REST controller for user administration endpoints.
 * <p>
 * Allows administrators to list users and manage roles through the service
 * layer while enforcing access checks with {@link RoleGuard}.
 */
public class UserController {

    /** Service used to read users and apply role changes. */
    private final UserService userService;

    /** Guard used to enforce admin-only access. */
    private final RoleGuard roleGuard;

    /**
     * Creates a new user controller.
     *
     * @param userService service used for user operations
     * @param roleGuard guard used to validate the caller's role
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed singleton beans injected via constructor")
    public UserController(UserService userService, RoleGuard roleGuard) {
        this.userService = userService;
        this.roleGuard = roleGuard;
    }

    /**
     * Returns the list of users visible to administrators.
     *
     * @param jwt authenticated principal token
     * @return list of user DTOs
     */
    @GetMapping
    public ResponseEntity<List<UserDTO>> getUsers(@AuthenticationPrincipal Jwt jwt) {
        roleGuard.requireRole(jwt, Role.ADMIN);
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Assigns a role to the target user.
     *
     * @param jwt authenticated principal token
     * @param id target user Auth0 identifier
     * @param dto request body containing the role to assign
     * @return 204 No Content when the assignment is accepted
     */
    @PostMapping("/{id}/roles")
    public ResponseEntity<Void> assignRole(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestBody RoleRequestDTO dto) {
        roleGuard.requireRole(jwt, Role.ADMIN);
        userService.assignRole(jwt.getSubject(), id, dto.role());
        return ResponseEntity.noContent().build();
    }

    /**
     * Removes a role from the target user.
     *
     * @param jwt authenticated principal token
     * @param id target user Auth0 identifier
     * @param dto request body containing the role to remove
     * @return 204 No Content when the removal is accepted
     */
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
