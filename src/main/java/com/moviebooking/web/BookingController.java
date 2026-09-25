package com.moviebooking.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.service.BookingService;
import com.moviebooking.web.dto.BookingResponse;
import com.moviebooking.web.dto.CreateBookingRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /** Step one of booking: hold the requested seats and create a booking in CREATED. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        return BookingResponse.from(
                bookingService.hold(request.showId(), request.userId(), request.seatIds()));
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable String id) {
        return BookingResponse.from(bookingService.get(id));
    }

    /** A user's booking history. */
    @GetMapping
    public List<BookingResponse> listByUser(@RequestParam String userId) {
        return bookingService.findByUser(userId).stream().map(BookingResponse::from).toList();
    }
}
