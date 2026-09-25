package com.moviebooking.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateMovieRequest(
        @NotBlank String name,
        @Positive int durationMinutes) {
}
