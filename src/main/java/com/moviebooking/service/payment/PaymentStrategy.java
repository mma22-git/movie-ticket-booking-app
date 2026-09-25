package com.moviebooking.service.payment;

import com.moviebooking.domain.Booking;

/**
 * Abstraction over a payment provider. Real gateways (card, UPI, wallet) would each be
 * an implementation; the booking flow depends only on this contract, so adding one does
 * not touch confirmation logic.
 */
public interface PaymentStrategy {

    /** Attempts to charge for the booking. Returns true if the payment succeeded. */
    boolean charge(Booking booking);
}
