package com.moviebooking.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A screening of a movie on a specific screen of a theater at a given start time.
 * References the movie and theater by id rather than embedding them, since both are
 * independent aggregates reused across many shows. {@code screenId} identifies the
 * screen within that theater whose seats this show sells.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "shows")
public class Show {

    @Id
    private String id;

    @Indexed
    private String movieId;

    @Indexed
    private String theaterId;

    private String screenId;

    private Instant startTime;

    private int durationMinutes;
}
