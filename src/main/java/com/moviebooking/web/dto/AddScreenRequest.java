package com.moviebooking.web.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** Adds a screen to a theater, optionally with its initial seats. */
public record AddScreenRequest(
        @NotBlank String name,
        @Valid List<SeatDto> seats) {
}
