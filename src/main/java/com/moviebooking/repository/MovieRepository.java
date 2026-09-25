package com.moviebooking.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.moviebooking.domain.Movie;

public interface MovieRepository extends MongoRepository<Movie, String> {
}
