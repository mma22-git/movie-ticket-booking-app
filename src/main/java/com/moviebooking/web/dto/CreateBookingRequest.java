package com.moviebooking.web.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Requests a hold on the given seats of a show (step one of booking). The booking is made
 * for the authenticated principal, so no user id is carried in the body.
 */
public record CreateBookingRequest(
        @NotBlank String showId,
        @NotEmpty List<@NotBlank String> seatIds) {
}
