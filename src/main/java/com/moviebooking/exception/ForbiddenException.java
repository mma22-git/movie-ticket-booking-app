package com.moviebooking.exception;

/**
 * Thrown when a caller tries to act on a booking they do not own. Mapped to HTTP 403.
 * (Full role-based access control is a later phase; this is the minimal ownership check.)
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
