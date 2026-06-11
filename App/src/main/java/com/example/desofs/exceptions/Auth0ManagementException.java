package com.example.desofs.exceptions;

/**
 * Raised when a call to the Auth0 Management API fails or returns a
 * non-success response.
 *
 * <p>Wrapped at the boundary so that internal HTTP error details are not
 * leaked through API responses.</p>
 */
public class Auth0ManagementException extends RuntimeException {
    public Auth0ManagementException(String message) {
        super(message);
    }

    public Auth0ManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}
