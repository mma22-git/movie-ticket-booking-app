package com.moviebooking.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.moviebooking.domain.Booking;

public interface BookingRepository extends MongoRepository<Booking, String> {

    List<Booking> findByUserId(String userId);

    List<Booking> findByShowId(String showId);
}
