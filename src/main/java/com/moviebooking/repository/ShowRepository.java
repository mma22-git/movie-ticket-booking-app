package com.moviebooking.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.moviebooking.domain.Show;

public interface ShowRepository extends MongoRepository<Show, String> {

    List<Show> findByMovieId(String movieId);

    List<Show> findByTheaterId(String theaterId);

    List<Show> findByMovieIdAndTheaterId(String movieId, String theaterId);
}
