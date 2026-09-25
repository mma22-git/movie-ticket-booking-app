package com.moviebooking.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Pays for a held booking. {@code paymentMethod} is accepted for realism but ignored by
 * the mocked gateway.
 */
public record PaymentRequest(
        @NotBlank String userId,
        String paymentMethod) {
}
