package com.moviebooking.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A seat within a theater's layout. Embedded inside {@link Theater} — a seat has no
 * identity or lifecycle of its own outside the theater it belongs to.
 *
 * {@code number} is the human-facing label (e.g. "A1") and is unique within a theater;
 * it is the identifier used when holding and booking seats for a show.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    private String number;
    private String row;
    private SeatCategory category;
}
