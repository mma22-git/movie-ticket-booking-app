package com.moviebooking.domain;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A screen (auditorium) within a theater, owning its own seat layout. Embedded inside
 * {@link Theater} — a screen has no lifecycle outside the theater it belongs to.
 *
 * {@code id} is unique within its theater; a {@link Show} points at one screen, and seat
 * numbers are unique within that screen.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Screen {

    private String id;

    private String name;

    @Builder.Default
    private List<Seat> seats = new ArrayList<>();
}
