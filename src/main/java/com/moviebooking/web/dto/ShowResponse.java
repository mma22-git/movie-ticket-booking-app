package com.moviebooking.web.dto;

import java.time.Instant;

import com.moviebooking.domain.Show;

public record ShowResponse(
        String id,
        String movieId,
        String theaterId,
        String screenId,
        Instant startTime,
        int durationMinutes) {

    public static ShowResponse from(Show show) {
        return new ShowResponse(
                show.getId(),
                show.getMovieId(),
                show.getTheaterId(),
                show.getScreenId(),
                show.getStartTime(),
                show.getDurationMinutes());
    }
}
