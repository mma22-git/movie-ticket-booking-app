package com.moviebooking.web.dto;

import com.moviebooking.domain.SeatCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** A seat to add to a screen. */
public record SeatDto(
        @NotBlank String number,
        String row,
        @NotNull SeatCategory category) {
}
