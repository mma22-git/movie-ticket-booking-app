package com.moviebooking.exception;

/**
 * Thrown when a requested entity (movie, theater, show, booking, ...) does not exist.
 * Mapped to HTTP 404 by the global exception handler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, String id) {
        return new ResourceNotFoundException(resource + " not found: " + id);
    }
}
