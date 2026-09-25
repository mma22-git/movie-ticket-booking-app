package com.moviebooking.service;

import java.time.Instant;

/**
 * An in-memory hold on a single seat: who holds it and when the hold lapses. Immutable;
 * a new lock replaces the old one when a seat is re-held after expiry.
 */
record SeatLock(String userId, Instant expiresAt) {

    boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
