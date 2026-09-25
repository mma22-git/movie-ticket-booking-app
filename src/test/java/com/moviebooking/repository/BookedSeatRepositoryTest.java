package com.moviebooking.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;

import com.moviebooking.domain.BookedSeat;

/**
 * Verifies the last line of defense: the unique (showId, seatId) index makes it
 * impossible to persist the same seat on the same show twice.
 */
@DataMongoTest
class BookedSeatRepositoryTest {

    @Autowired
    private BookedSeatRepository bookedSeatRepository;

    @BeforeEach
    void clearDocuments() {
        // Remove documents but keep the collection (and its index) in place.
        bookedSeatRepository.deleteAll();
    }

    @Test
    void rejectsDuplicateSeatOnSameShow() {
        bookedSeatRepository.save(bookedSeat("show-1", "A1", "booking-1"));

        BookedSeat duplicate = bookedSeat("show-1", "A1", "booking-2");

        assertThatThrownBy(() -> bookedSeatRepository.save(duplicate))
                .isInstanceOf(DuplicateKeyException.class);

        assertThat(bookedSeatRepository.findByShowId("show-1")).hasSize(1);
    }

    @Test
    void allowsSameSeatNumberOnDifferentShows() {
        bookedSeatRepository.save(bookedSeat("show-1", "A1", "booking-1"));
        bookedSeatRepository.save(bookedSeat("show-2", "A1", "booking-2"));

        assertThat(bookedSeatRepository.findAll()).hasSize(2);
    }

    private BookedSeat bookedSeat(String showId, String seatId, String bookingId) {
        return BookedSeat.builder()
                .showId(showId)
                .seatId(seatId)
                .bookingId(bookingId)
                .userId("user-1")
                .createdAt(Instant.now())
                .build();
    }
}
