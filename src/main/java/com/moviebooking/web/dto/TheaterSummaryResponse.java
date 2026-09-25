package com.moviebooking.web.dto;

import com.moviebooking.domain.Theater;

/** A lightweight theater view (no screens/seats) for discovery listings. */
public record TheaterSummaryResponse(String id, String name, String city) {

    public static TheaterSummaryResponse from(Theater theater) {
        return new TheaterSummaryResponse(theater.getId(), theater.getName(), theater.getCity());
    }
}
