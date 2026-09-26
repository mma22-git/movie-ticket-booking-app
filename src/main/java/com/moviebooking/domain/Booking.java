package com.moviebooking.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A user's booking for a set of seats on one show. The seat identifiers are embedded,
 * since a booking is always read and written as a single unit.
 *
 * A booking starts in {@link BookingStatus#CREATED} when the seats are held, and moves
 * to {@link BookingStatus#CONFIRMED} once payment succeeds. Committing the seats so no
 * one else can take them is the job of {@link BookedSeat}, not this document.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "bookings")
public class Booking {

    @Id
    private String id;

    @Indexed
    private String showId;

    @Indexed
    private String userId;

    @Builder.Default
    private List<String> seatIds = new ArrayList<>();

    private BookingStatus status;

    private Instant createdAt;
}
