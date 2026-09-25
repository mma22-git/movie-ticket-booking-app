package com.moviebooking.web.dto;

import java.util.List;

import com.moviebooking.domain.Screen;

public record ScreenResponse(String id, String name, List<SeatResponse> seats) {

    public static ScreenResponse from(Screen screen) {
        List<SeatResponse> seats = screen.getSeats().stream()
                .map(SeatResponse::from)
                .toList();
        return new ScreenResponse(screen.getId(), screen.getName(), seats);
    }
}
