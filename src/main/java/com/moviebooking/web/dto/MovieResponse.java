package com.moviebooking.web.dto;

import com.moviebooking.domain.Movie;

public record MovieResponse(String id, String name, int durationMinutes) {

    public static MovieResponse from(Movie movie) {
        return new MovieResponse(movie.getId(), movie.getName(), movie.getDurationMinutes());
    }
}
