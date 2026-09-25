package com.moviebooking.web;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.service.PaymentService;
import com.moviebooking.web.dto.BookingResponse;
import com.moviebooking.web.dto.PaymentRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings/{bookingId}/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** Step two of booking: pay (mocked) and confirm the held booking. */
    @PostMapping
    public BookingResponse pay(@PathVariable String bookingId, @Valid @RequestBody PaymentRequest request) {
        return BookingResponse.from(paymentService.pay(bookingId, request.userId()));
    }
}
