package com.example.desofs.shared.dtos;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * Data Transfer Object representing an Auth0 user in API responses.
 *
 * <p>Contains only the public fields exposed by the user-administration
 * endpoints (UC8). The identifier is the Auth0 user id (e.g.
 * {@code auth0|abc123}), not a database primary key.</p>
 */
public class UserDTO {

    private String userId;
    private String email;
    private String name;
    private List<String> roles;

    public UserDTO() {
        this.roles = List.of();
    }

    public UserDTO(String userId, String email, String name) {
        this(userId, email, name, List.of());
    }

    public UserDTO(String userId, String email, String name, List<String> roles) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "Returned list is constructed via List.copyOf")
    public List<String> getRoles() { return roles; }

    public void setRoles(List<String> roles) {
        this.roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
