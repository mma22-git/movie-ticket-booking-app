package com.moviebooking.web.dto;

/**
 * Pays for a held booking. The payer is the authenticated principal; {@code paymentMethod}
 * is accepted for realism but ignored by the mocked gateway.
 */
public record PaymentRequest(String paymentMethod) {
}
