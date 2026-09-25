package com.moviebooking.exception;

/**
 * Thrown when a request conflicts with existing state — e.g. a duplicate screen or seat
 * number, or a seat that is already taken. Mapped to HTTP 409 by the global handler.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
