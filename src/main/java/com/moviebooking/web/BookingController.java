package com.moviebooking.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.domain.Role;
import com.moviebooking.security.RequiresRole;
import com.moviebooking.security.RoleAuthorizationInterceptor;
import com.moviebooking.service.BookingService;
import com.moviebooking.web.dto.BookingResponse;
import com.moviebooking.web.dto.CreateBookingRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@RequiresRole(Role.CUSTOMER)
public class BookingController {

    private final BookingService bookingService;

    /** Step one of booking: hold the requested seats for the caller and create a CREATED booking. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request,
            @RequestAttribute(RoleAuthorizationInterceptor.PRINCIPAL_ATTRIBUTE) String userId) {
        return BookingResponse.from(bookingService.hold(request.showId(), userId, request.seatIds()));
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable String id,
            @RequestAttribute(RoleAuthorizationInterceptor.PRINCIPAL_ATTRIBUTE) String userId) {
        return BookingResponse.from(bookingService.getOwned(id, userId));
    }

    /** The caller's own booking history. */
    @GetMapping
    public List<BookingResponse> myBookings(
            @RequestAttribute(RoleAuthorizationInterceptor.PRINCIPAL_ATTRIBUTE) String userId) {
        return bookingService.findByUser(userId).stream().map(BookingResponse::from).toList();
    }
}
