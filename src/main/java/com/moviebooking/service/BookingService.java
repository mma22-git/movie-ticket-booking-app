package com.moviebooking.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.moviebooking.domain.BookedSeat;
import com.moviebooking.domain.Booking;
import com.moviebooking.domain.BookingStatus;
import com.moviebooking.domain.Seat;
import com.moviebooking.domain.Show;
import com.moviebooking.exception.ConflictException;
import com.moviebooking.exception.ForbiddenException;
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

    /**
     * Step two of booking: commit a held booking once payment has succeeded.
     *
     * <p>Order matters. We write one {@link BookedSeat} per seat <em>first</em>: the
     * unique {@code (showId, seatId)} index is the durable guarantee, so a duplicate-key
     * error here means the seat was taken and the whole confirmation aborts (any rows we
     * did insert are removed, since standalone Mongo gives no multi-document
     * transaction). Only after all seat rows are in do we flip the booking to CONFIRMED,
     * compensating the same way if that write fails. Finally the in-memory holds are
     * released — the seats are now durably ours.
     */
    public Booking confirm(String bookingId, String userId) {
        Booking booking = get(bookingId);
        if (!booking.getUserId().equals(userId)) {
            throw new ForbiddenException("Booking belongs to another user");
        }
        if (booking.getStatus() != BookingStatus.CREATED) {
            throw new ConflictException("Booking is not awaiting confirmation: " + booking.getStatus());
        }

        String showId = booking.getShowId();
        List<String> seatIds = booking.getSeatIds();
        for (String seatId : seatIds) {
            if (!seatLockProvider.isHeldBy(showId, seatId, userId)) {
                throw new SeatUnavailableException("Hold has expired for seat: " + seatId);
            }
        }

        persistBookedSeats(booking);

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking confirmed;
        try {
            confirmed = bookingRepository.save(booking);
        } catch (RuntimeException e) {
            bookedSeatRepository.deleteByBookingId(bookingId); // undo the seat rows
            throw e;
        }

        seatLockProvider.release(showId, seatIds, userId);
        return confirmed;
    }

    public Booking get(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));
    }

    /** Fetches a booking, ensuring it belongs to the requesting user. */
    public Booking getOwned(String bookingId, String userId) {
        Booking booking = get(bookingId);
        if (!booking.getUserId().equals(userId)) {
            throw new ForbiddenException("Booking belongs to another user");
        }
        return booking;
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

    private void persistBookedSeats(Booking booking) {
        Instant now = Instant.now();
        List<BookedSeat> rows = booking.getSeatIds().stream()
                .map(seatId -> BookedSeat.builder()
                        .showId(booking.getShowId())
                        .seatId(seatId)
                        .bookingId(booking.getId())
                        .userId(booking.getUserId())
                        .createdAt(now)
                        .build())
                .toList();
        try {
            bookedSeatRepository.saveAll(rows);
        } catch (DuplicateKeyException e) {
            // Someone committed one of these seats first; undo any rows we inserted.
            bookedSeatRepository.deleteByBookingId(booking.getId());
            throw new SeatUnavailableException("One or more seats were just booked by someone else");
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
