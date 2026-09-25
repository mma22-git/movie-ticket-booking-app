package com.moviebooking.domain;

/**
 * Lifecycle of a booking.
 *
 * CREATED   - seats are held for the user, payment not yet completed.
 * CONFIRMED - payment succeeded and the seats are committed to the user.
 * EXPIRED   - the hold lapsed before payment completed.
 */
public enum BookingStatus {
    CREATED,
    CONFIRMED,
    EXPIRED
}
