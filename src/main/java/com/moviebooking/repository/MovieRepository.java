package com.moviebooking.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.moviebooking.domain.Movie;

public interface MovieRepository extends MongoRepository<Movie, String> {

    List<Movie> findByNameContainingIgnoreCase(String name);
}
