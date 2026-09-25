package com.moviebooking.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.moviebooking.domain.Theater;

public interface TheaterRepository extends MongoRepository<Theater, String> {
}
