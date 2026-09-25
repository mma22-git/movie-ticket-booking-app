package com.moviebooking.web.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/** Adds one or more seats to an existing screen. */
public record AddSeatsRequest(
        @NotEmpty @Valid List<SeatDto> seats) {
}
