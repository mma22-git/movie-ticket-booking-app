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

import com.moviebooking.domain.Role;
import com.moviebooking.security.RequiresRole;
import com.moviebooking.service.ShowService;
import com.moviebooking.web.dto.CreateShowRequest;
import com.moviebooking.web.dto.SeatResponse;
import com.moviebooking.web.dto.ShowResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresRole(Role.ADMIN)
    public ShowResponse create(@Valid @RequestBody CreateShowRequest request) {
        return ShowResponse.from(showService.create(request));
    }

    @GetMapping("/{id}")
    @RequiresRole({Role.ADMIN, Role.CUSTOMER})
    public ShowResponse get(@PathVariable String id) {
        return ShowResponse.from(showService.get(id));
    }

    /** Lists shows, optionally narrowed by movie and/or theater (the browse flow). */
    @GetMapping
    @RequiresRole({Role.ADMIN, Role.CUSTOMER})
    public List<ShowResponse> list(@RequestParam(required = false) String movieId,
            @RequestParam(required = false) String theaterId) {
        return showService.find(movieId, theaterId).stream().map(ShowResponse::from).toList();
    }

    @GetMapping("/{id}/available-seats")
    @RequiresRole({Role.ADMIN, Role.CUSTOMER})
    public List<SeatResponse> availableSeats(@PathVariable String id) {
        return showService.getAvailableSeats(id).stream().map(SeatResponse::from).toList();
    }
}
