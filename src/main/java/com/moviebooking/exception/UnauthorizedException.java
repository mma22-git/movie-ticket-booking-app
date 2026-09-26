package com.moviebooking.exception;

/**
 * Thrown when a request that requires an authenticated principal is missing a valid one
 * (no or unknown {@code X-User-Id}). Mapped to HTTP 401.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
