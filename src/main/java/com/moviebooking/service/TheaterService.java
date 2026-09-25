package com.moviebooking.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moviebooking.domain.Screen;
import com.moviebooking.domain.Seat;
import com.moviebooking.domain.Theater;
import com.moviebooking.exception.ConflictException;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.TheaterRepository;
import com.moviebooking.web.dto.AddScreenRequest;
import com.moviebooking.web.dto.AddSeatsRequest;
import com.moviebooking.web.dto.SeatDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TheaterService {

    private final TheaterRepository theaterRepository;

    public Theater create(String name, String city) {
        Theater theater = Theater.builder().name(name).city(city).build();
        return theaterRepository.save(theater);
    }

    public Theater get(String id) {
        return theaterRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Theater", id));
    }

    public List<Theater> getAllById(Collection<String> ids) {
        return theaterRepository.findAllById(ids);
    }

    /** Adds a screen (with any initial seats) to a theater and returns the new screen. */
    public Screen addScreen(String theaterId, AddScreenRequest request) {
        Theater theater = get(theaterId);

        List<Seat> seats = toSeats(request.seats() == null ? List.of() : request.seats());
        Screen screen = Screen.builder()
                .id(UUID.randomUUID().toString())
                .name(request.name())
                .seats(seats)
                .build();

        theater.getScreens().add(screen);
        theaterRepository.save(theater);
        return screen;
    }

    /** Adds seats to an existing screen, rejecting seat numbers already used on it. */
    public Screen addSeats(String theaterId, String screenId, AddSeatsRequest request) {
        Theater theater = get(theaterId);
        Screen screen = findScreen(theater, screenId);

        List<String> existingNumbers = screen.getSeats().stream().map(Seat::getNumber).toList();
        for (SeatDto seat : request.seats()) {
            if (existingNumbers.contains(seat.number())) {
                throw new ConflictException("Seat already exists on screen: " + seat.number());
            }
        }

        screen.getSeats().addAll(toSeats(request.seats()));
        theaterRepository.save(theater);
        return screen;
    }

    public Screen findScreen(Theater theater, String screenId) {
        return theater.getScreens().stream()
                .filter(screen -> screen.getId().equals(screenId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Screen", screenId));
    }

    private List<Seat> toSeats(List<SeatDto> seatDtos) {
        List<Seat> seats = new ArrayList<>();
        List<String> numbers = new ArrayList<>();
        for (SeatDto dto : seatDtos) {
            if (numbers.contains(dto.number())) {
                throw new ConflictException("Duplicate seat number in request: " + dto.number());
            }
            numbers.add(dto.number());
            seats.add(Seat.builder()
                    .number(dto.number())
                    .row(dto.row())
                    .category(dto.category())
                    .build());
        }
        return seats;
    }
}
