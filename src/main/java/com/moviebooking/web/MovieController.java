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
import com.moviebooking.service.MovieService;
import com.moviebooking.service.ShowService;
import com.moviebooking.web.dto.CreateMovieRequest;
import com.moviebooking.web.dto.MovieResponse;
import com.moviebooking.web.dto.TheaterSummaryResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final ShowService showService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresRole(Role.ADMIN)
    public MovieResponse create(@Valid @RequestBody CreateMovieRequest request) {
        return MovieResponse.from(movieService.create(request));
    }

    @GetMapping("/{id}")
    @RequiresRole({Role.ADMIN, Role.CUSTOMER})
    public MovieResponse get(@PathVariable String id) {
        return MovieResponse.from(movieService.get(id));
    }

    /** Lists movies, optionally filtered by a case-insensitive partial name search. */
    @GetMapping
    @RequiresRole({Role.ADMIN, Role.CUSTOMER})
    public List<MovieResponse> search(@RequestParam(required = false) String name) {
        return movieService.search(name).stream().map(MovieResponse::from).toList();
    }

    /** Theaters currently screening this movie, optionally filtered by city. */
    @GetMapping("/{id}/theaters")
    @RequiresRole({Role.ADMIN, Role.CUSTOMER})
    public List<TheaterSummaryResponse> theatersScreening(@PathVariable String id,
            @RequestParam(required = false) String city) {
        return showService.getTheatersScreeningMovie(id, city).stream()
                .map(TheaterSummaryResponse::from)
                .toList();
    }
}
