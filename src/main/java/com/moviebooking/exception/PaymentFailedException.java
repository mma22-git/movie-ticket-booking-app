package com.moviebooking.exception;

/**
 * Thrown when the (mocked) payment gateway declines a charge. Mapped to HTTP 402.
 * The gateway is stubbed to succeed, but the failure path is modeled so confirmation
 * only happens on a successful charge.
 */
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(String message) {
        super(message);
    }
}
