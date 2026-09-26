package com.moviebooking.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.domain.Role;
import com.moviebooking.security.RequiresRole;
import com.moviebooking.service.TheaterService;
import com.moviebooking.web.dto.AddScreenRequest;
import com.moviebooking.web.dto.AddSeatsRequest;
import com.moviebooking.web.dto.CreateTheaterRequest;
import com.moviebooking.web.dto.ScreenResponse;
import com.moviebooking.web.dto.TheaterResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/theaters")
@RequiredArgsConstructor
public class TheaterController {

    private final TheaterService theaterService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresRole(Role.ADMIN)
    public TheaterResponse create(@Valid @RequestBody CreateTheaterRequest request) {
        return TheaterResponse.from(theaterService.create(request.name(), request.city()));
    }

    @GetMapping("/{id}")
    @RequiresRole({Role.ADMIN, Role.CUSTOMER})
    public TheaterResponse get(@PathVariable String id) {
        return TheaterResponse.from(theaterService.get(id));
    }

    @PostMapping("/{id}/screens")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresRole(Role.ADMIN)
    public ScreenResponse addScreen(@PathVariable String id, @Valid @RequestBody AddScreenRequest request) {
        return ScreenResponse.from(theaterService.addScreen(id, request));
    }

    @PostMapping("/{id}/screens/{screenId}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresRole(Role.ADMIN)
    public ScreenResponse addSeats(@PathVariable String id, @PathVariable String screenId,
            @Valid @RequestBody AddSeatsRequest request) {
        return ScreenResponse.from(theaterService.addSeats(id, screenId, request));
    }
}
