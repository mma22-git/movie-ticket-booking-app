package com.moviebooking.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTheaterRequest(
        @NotBlank String name,
        @NotBlank String city) {
}
