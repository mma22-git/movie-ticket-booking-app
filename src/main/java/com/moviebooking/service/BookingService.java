package com.moviebooking.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moviebooking.domain.Booking;
import com.moviebooking.domain.BookingStatus;
import com.moviebooking.domain.Seat;
import com.moviebooking.domain.Show;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.exception.SeatUnavailableException;
import com.moviebooking.repository.BookedSeatRepository;
import com.moviebooking.repository.BookingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookedSeatRepository bookedSeatRepository;
    private final ShowService showService;
    private final UserService userService;
    private final SeatLockProvider seatLockProvider;

    /**
     * Step one of booking: hold the requested seats for the user and record a booking in
     * {@link BookingStatus#CREATED}. Payment and confirmation happen separately.
     *
     * <p>The in-process hold is what serializes concurrent attempts on the same seat; the
     * checks before it are fast-fail guards. If persisting the booking fails after the
     * hold is taken, the hold is released so the seats are not stranded.
     */
    public Booking hold(String showId, String userId, List<String> seatIds) {
        List<String> distinctSeatIds = seatIds.stream().distinct().toList();
        if (distinctSeatIds.size() != seatIds.size()) {
            throw new IllegalArgumentException("Duplicate seat ids in request");
        }

        Show show = showService.get(showId);
        userService.get(userId); // validates the user exists (404 otherwise)

        validateSeatsExistOnScreen(show, distinctSeatIds);
        rejectAlreadyBooked(showId, distinctSeatIds);

        seatLockProvider.hold(showId, distinctSeatIds, userId);
        try {
            Booking booking = Booking.builder()
                    .showId(showId)
                    .userId(userId)
                    .seatIds(distinctSeatIds)
                    .status(BookingStatus.CREATED)
                    .createdAt(Instant.now())
                    .build();
            return bookingRepository.save(booking);
        } catch (RuntimeException e) {
            // Don't strand the seats if we couldn't record the booking.
            seatLockProvider.release(showId, distinctSeatIds, userId);
            throw e;
        }
    }

    public Booking get(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));
    }

    public List<Booking> findByUser(String userId) {
        return bookingRepository.findByUserId(userId);
    }

    private void validateSeatsExistOnScreen(Show show, List<String> seatIds) {
        Set<String> screenSeatNumbers = showService.getScreenSeats(show).stream()
                .map(Seat::getNumber)
                .collect(Collectors.toSet());
        for (String seatId : seatIds) {
            if (!screenSeatNumbers.contains(seatId)) {
                throw ResourceNotFoundException.of("Seat", seatId);
            }
        }
    }

    private void rejectAlreadyBooked(String showId, List<String> seatIds) {
        Set<String> booked = bookedSeatRepository.findByShowId(showId).stream()
                .map(com.moviebooking.domain.BookedSeat::getSeatId)
                .collect(Collectors.toSet());
        for (String seatId : seatIds) {
            if (booked.contains(seatId)) {
                throw new SeatUnavailableException("Seat already booked: " + seatId);
            }
        }
    }
}
