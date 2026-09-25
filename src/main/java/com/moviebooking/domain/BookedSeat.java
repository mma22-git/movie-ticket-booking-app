package com.moviebooking.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One record per seat that has been committed to a booking, existing solely to enforce
 * that a given seat on a given show can belong to at most one booking.
 *
 * The unique compound index on {@code (showId, seatId)} is the last line of defense
 * against double-allocation: even if the in-process hold were bypassed or buggy, a
 * second insert for the same seat fails with a duplicate-key error, so a double-booking
 * can never be persisted.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "booked_seats")
@CompoundIndex(name = "uq_show_seat", def = "{'showId': 1, 'seatId': 1}", unique = true)
public class BookedSeat {

    @Id
    private String id;

    private String showId;

    private String seatId;

    private String bookingId;

    private String userId;

    private Instant createdAt;
}
