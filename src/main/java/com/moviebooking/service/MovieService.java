package com.moviebooking.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moviebooking.domain.Movie;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.web.dto.CreateMovieRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    public Movie create(CreateMovieRequest request) {
        Movie movie = Movie.builder()
                .name(request.name())
                .durationMinutes(request.durationMinutes())
                .build();
        return movieRepository.save(movie);
    }

    public Movie get(String id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Movie", id));
    }

    /**
     * Lists movies, optionally filtered by a case-insensitive partial name match. A blank
     * or missing name returns all movies.
     */
    public List<Movie> search(String name) {
        if (name == null || name.isBlank()) {
            return movieRepository.findAll();
        }
        return movieRepository.findByNameContainingIgnoreCase(name.trim());
    }
}
