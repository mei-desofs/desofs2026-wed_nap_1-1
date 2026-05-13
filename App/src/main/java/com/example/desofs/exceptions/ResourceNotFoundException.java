package com.example.desofs.exceptions;

/**
 * Exception thrown when a requested resource cannot be found.
 *
 * <p>Optionally carries the resource type and identifier to support
 * consistent error reporting and diagnostics.</p>
 */
public class ResourceNotFoundException extends RuntimeException {
    private String resourceType;
    private Object resourceId;

    /**
     * Creates a not-found exception with a human-readable message.
     *
     * @param message error message describing the missing resource
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a not-found exception with a message and resource metadata.
     *
     * @param message error message describing the missing resource
     * @param resourceType logical type of the missing resource
     * @param resourceId identifier of the missing resource
     */
    public ResourceNotFoundException(String message, String resourceType, Object resourceId) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Returns the logical type of the missing resource.
     *
     * @return resource type, or {@code null} when not provided
     */
    public String getResourceType() { return resourceType; }

    /**
     * Returns the identifier of the missing resource.
     *
     * @return resource identifier, or {@code null} when not provided
     */
    public Object getResourceId() { return resourceId; }
}
