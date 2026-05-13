package com.example.desofs.exceptions;

/**
 * Custom exception for resource not found
 */
public class ResourceNotFoundException extends RuntimeException {
    private String resourceType;
    private Object resourceId;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, String resourceType, Object resourceId) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() { return resourceType; }
    public Object getResourceId() { return resourceId; }
}
