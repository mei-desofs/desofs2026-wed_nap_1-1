package com.example.desofs.shared.dtos;

/**
 * Data Transfer Object representing a user in API responses.
 *
 * <p>Contains the user identifier and the public profile fields exposed by
 * the application.</p>
 */
public class UserDTO {
    /** User identifier. */
    private Long id;

    /** User email address. */
    private String email;

    /** User display name. */
    private String name;

    /**
     * Creates an empty user DTO.
     */
    public UserDTO() {}

    /**
     * Creates a user DTO with all fields populated.
     *
     * @param id user identifier
     * @param email user email address
     * @param name user display name
     */
    public UserDTO(Long id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }

    /**
     * Returns the user identifier.
     *
     * @return user identifier
     */
    public Long getId() { return id; }

    /**
     * Sets the user identifier.
     *
     * @param id user identifier
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Returns the user email address.
     *
     * @return user email address
     */
    public String getEmail() { return email; }

    /**
     * Sets the user email address.
     *
     * @param email user email address
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Returns the user display name.
     *
     * @return user display name
     */
    public String getName() { return name; }

    /**
     * Sets the user display name.
     *
     * @param name user display name
     */
    public void setName(String name) { this.name = name; }
}
