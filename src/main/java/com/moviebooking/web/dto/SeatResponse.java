package com.moviebooking.web.dto;

import com.moviebooking.domain.Seat;
import com.moviebooking.domain.SeatCategory;

public record SeatResponse(String number, String row, SeatCategory category) {

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getNumber(), seat.getRow(), seat.getCategory());
    }
}
