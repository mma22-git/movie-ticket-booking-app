package com.moviebooking.web.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateShowRequest(
        @NotBlank String movieId,
        @NotBlank String theaterId,
        @NotBlank String screenId,
        @NotNull Instant startTime,
        @Positive int durationMinutes) {
}
