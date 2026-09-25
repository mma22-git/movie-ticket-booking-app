package com.moviebooking.service.payment;

import org.springframework.stereotype.Component;

import com.moviebooking.domain.Booking;

/**
 * Stubbed payment provider used for this exercise: always approves the charge. A real
 * integration (calling an external gateway) would replace this without changing the
 * booking flow.
 */
@Component
public class MockPaymentStrategy implements PaymentStrategy {

    @Override
    public boolean charge(Booking booking) {
        return true;
    }
}
