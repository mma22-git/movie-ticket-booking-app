package com.moviebooking.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.moviebooking.exception.SeatUnavailableException;

/**
 * In-process seat holds — the primary concurrency control for booking.
 *
 * <p>Locks are stored as {@code showId -> (seatId -> SeatLock)}. The outer map is a
 * {@link ConcurrentHashMap} so the per-show lock table can be created once, atomically,
 * regardless of how many threads race. Every read or write of a show's inner table
 * happens inside {@code synchronized(seatLocks)}, so all hold, release, and validate
 * operations for a single show are serialized on that table's monitor — two users racing
 * for the same seat cannot both pass the availability check.
 *
 * <p>Seats are keyed by their string ids (never by object identity), and ownership is
 * tracked by {@code userId}. Expiry is lazy: a lock whose {@code expiresAt} has passed is
 * treated as absent on the next read, so an abandoned hold frees its seats automatically
 * without a background sweeper.
 *
 * <p>This is correct for a single-instance deployment; a horizontally scaled deployment
 * would need a shared lock (e.g. Redis), which is out of scope here. The unique
 * {@code (showId, seatId)} index remains the durable backstop regardless.
 */
@Component
public class SeatLockProvider {

    private final long holdTimeoutSeconds;
    private final Map<String, Map<String, SeatLock>> locksByShow = new ConcurrentHashMap<>();

    public SeatLockProvider(@Value("${booking.hold.timeout-seconds:120}") long holdTimeoutSeconds) {
        this.holdTimeoutSeconds = holdTimeoutSeconds;
    }

    /**
     * Holds all the given seats for the user, or none of them. Fails if any seat is
     * currently held by anyone (including a still-valid hold by the same user).
     */
    public void hold(String showId, List<String> seatIds, String userId) {
        Map<String, SeatLock> seatLocks = locksByShow.computeIfAbsent(showId, k -> new ConcurrentHashMap<>());
        Instant now = Instant.now();
        synchronized (seatLocks) {
            for (String seatId : seatIds) {
                SeatLock existing = seatLocks.get(seatId);
                if (existing != null && !existing.isExpired(now)) {
                    throw new SeatUnavailableException("Seat is currently held: " + seatId);
                }
            }
            Instant expiresAt = now.plusSeconds(holdTimeoutSeconds);
            for (String seatId : seatIds) {
                seatLocks.put(seatId, new SeatLock(userId, expiresAt));
            }
        }
    }

    /** Releases the given seats, but only those still held by this user. */
    public void release(String showId, List<String> seatIds, String userId) {
        Map<String, SeatLock> seatLocks = locksByShow.get(showId);
        if (seatLocks == null) {
            return;
        }
        synchronized (seatLocks) {
            for (String seatId : seatIds) {
                SeatLock lock = seatLocks.get(seatId);
                if (lock != null && lock.userId().equals(userId)) {
                    seatLocks.remove(seatId);
                }
            }
        }
    }

    /** True if the seat is currently held by this user and the hold has not expired. */
    public boolean isHeldBy(String showId, String seatId, String userId) {
        Map<String, SeatLock> seatLocks = locksByShow.get(showId);
        if (seatLocks == null) {
            return false;
        }
        synchronized (seatLocks) {
            SeatLock lock = seatLocks.get(seatId);
            return lock != null && !lock.isExpired(Instant.now()) && lock.userId().equals(userId);
        }
    }

    /** Seat ids on the show that currently have a valid (non-expired) hold. */
    public Set<String> heldSeatIds(String showId) {
        Map<String, SeatLock> seatLocks = locksByShow.get(showId);
        if (seatLocks == null) {
            return Set.of();
        }
        Instant now = Instant.now();
        synchronized (seatLocks) {
            return seatLocks.entrySet().stream()
                    .filter(entry -> !entry.getValue().isExpired(now))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        }
    }
}
