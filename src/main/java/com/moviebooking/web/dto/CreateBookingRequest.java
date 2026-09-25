package com.moviebooking.web.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/** Requests a hold on the given seats of a show for a user (step one of booking). */
public record CreateBookingRequest(
        @NotBlank String showId,
        @NotBlank String userId,
        @NotEmpty List<@NotBlank String> seatIds) {
}
