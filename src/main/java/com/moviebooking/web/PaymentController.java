package com.moviebooking.web;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.domain.Role;
import com.moviebooking.security.RequiresRole;
import com.moviebooking.security.RoleAuthorizationInterceptor;
import com.moviebooking.service.PaymentService;
import com.moviebooking.web.dto.BookingResponse;
import com.moviebooking.web.dto.PaymentRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings/{bookingId}/payment")
@RequiredArgsConstructor
@RequiresRole(Role.CUSTOMER)
public class PaymentController {

    private final PaymentService paymentService;

    /** Step two of booking: pay (mocked) and confirm the caller's held booking. */
    @PostMapping
    public BookingResponse pay(@PathVariable String bookingId,
            @RequestBody(required = false) PaymentRequest request,
            @RequestAttribute(RoleAuthorizationInterceptor.PRINCIPAL_ATTRIBUTE) String userId) {
        return BookingResponse.from(paymentService.pay(bookingId, userId));
    }
}
