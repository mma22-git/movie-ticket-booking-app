package com.moviebooking.service;

import org.springframework.stereotype.Service;

import com.moviebooking.domain.Booking;
import com.moviebooking.exception.ForbiddenException;
import com.moviebooking.exception.PaymentFailedException;
import com.moviebooking.service.payment.PaymentStrategy;

import lombok.RequiredArgsConstructor;

/**
 * Takes payment for a held booking and, on success, confirms it. Payment itself is
 * delegated to a {@link PaymentStrategy} (mocked for this exercise); confirmation and the
 * seat-commit guarantee live in {@link BookingService}.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingService bookingService;
    private final PaymentStrategy paymentStrategy;

    public Booking pay(String bookingId, String userId) {
        Booking booking = bookingService.get(bookingId);
        if (!booking.getUserId().equals(userId)) {
            throw new ForbiddenException("Booking belongs to another user");
        }

        if (!paymentStrategy.charge(booking)) {
            throw new PaymentFailedException("Payment was declined for booking: " + bookingId);
        }

        return bookingService.confirm(bookingId, userId);
    }
}
