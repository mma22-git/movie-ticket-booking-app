package com.moviebooking.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.moviebooking.domain.BookedSeat;

public interface BookedSeatRepository extends MongoRepository<BookedSeat, String> {

    List<BookedSeat> findByShowId(String showId);

    void deleteByBookingId(String bookingId);
}
