package com.moviebooking.exception;

/**
 * Thrown when a requested seat cannot be held or booked because it is already held by
 * another user or already committed to a booking. A {@link ConflictException}, so it maps
 * to HTTP 409.
 */
public class SeatUnavailableException extends ConflictException {

    public SeatUnavailableException(String message) {
        super(message);
    }
}
