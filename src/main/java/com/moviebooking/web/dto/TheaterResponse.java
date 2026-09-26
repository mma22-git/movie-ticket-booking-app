package com.moviebooking.web.dto;

import java.util.List;

import com.moviebooking.domain.Theater;

public record TheaterResponse(String id, String name, String city, List<ScreenResponse> screens) {

    public static TheaterResponse from(Theater theater) {
        List<ScreenResponse> screens = theater.getScreens().stream()
                .map(ScreenResponse::from)
                .toList();
        return new TheaterResponse(theater.getId(), theater.getName(), theater.getCity(), screens);
    }
}
