package com.moviebooking.web.dto;

import java.time.Instant;
import java.util.List;

import com.moviebooking.domain.Booking;
import com.moviebooking.domain.BookingStatus;

public record BookingResponse(
        String id,
        String showId,
        String userId,
        List<String> seatIds,
        BookingStatus status,
        Instant createdAt) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getShowId(),
                booking.getUserId(),
                booking.getSeatIds(),
                booking.getStatus(),
                booking.getCreatedAt());
    }
}
