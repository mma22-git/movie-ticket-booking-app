package com.moviebooking.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moviebooking.domain.BookedSeat;
import com.moviebooking.domain.Screen;
import com.moviebooking.domain.Seat;
import com.moviebooking.domain.Show;
import com.moviebooking.domain.Theater;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.BookedSeatRepository;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.ShowRepository;
import com.moviebooking.web.dto.CreateShowRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final TheaterService theaterService;
    private final BookedSeatRepository bookedSeatRepository;
    private final SeatLockProvider seatLockProvider;

    public Show create(CreateShowRequest request) {
        if (!movieRepository.existsById(request.movieId())) {
            throw ResourceNotFoundException.of("Movie", request.movieId());
        }
        // Validates both the theater and that the screen belongs to it.
        Theater theater = theaterService.get(request.theaterId());
        theaterService.findScreen(theater, request.screenId());

        Show show = Show.builder()
                .movieId(request.movieId())
                .theaterId(request.theaterId())
                .screenId(request.screenId())
                .startTime(request.startTime())
                .durationMinutes(request.durationMinutes())
                .build();
        return showRepository.save(show);
    }

    public Show get(String id) {
        return showRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Show", id));
    }

    /**
     * Lists shows, optionally narrowed by movie and/or theater. Used by the browse flow:
     * shows for a movie, shows at a theater, or shows for a movie at a specific theater.
     */
    public List<Show> find(String movieId, String theaterId) {
        boolean hasMovie = movieId != null && !movieId.isBlank();
        boolean hasTheater = theaterId != null && !theaterId.isBlank();

        if (hasMovie && hasTheater) {
            return showRepository.findByMovieIdAndTheaterId(movieId, theaterId);
        }
        if (hasMovie) {
            return showRepository.findByMovieId(movieId);
        }
        if (hasTheater) {
            return showRepository.findByTheaterId(theaterId);
        }
        return showRepository.findAll();
    }

    /**
     * Theaters that have at least one show of the given movie, optionally filtered by
     * city. Backs the "where is this movie playing?" step of the browse flow.
     */
    public List<Theater> getTheatersScreeningMovie(String movieId, String city) {
        if (!movieRepository.existsById(movieId)) {
            throw ResourceNotFoundException.of("Movie", movieId);
        }
        Set<String> theaterIds = showRepository.findByMovieId(movieId).stream()
                .map(Show::getTheaterId)
                .collect(Collectors.toSet());

        return theaterService.getAllById(theaterIds).stream()
                .filter(theater -> city == null || city.isBlank() || city.equalsIgnoreCase(theater.getCity()))
                .toList();
    }

    /** All seats belonging to the screen this show plays on. */
    public List<Seat> getScreenSeats(Show show) {
        Theater theater = theaterService.get(show.getTheaterId());
        Screen screen = theaterService.findScreen(theater, show.getScreenId());
        return screen.getSeats();
    }

    /**
     * Seats on the show's screen that a customer can still pick: the screen's seats minus
     * those already committed to a booking and minus those currently held by someone.
     */
    public List<Seat> getAvailableSeats(String showId) {
        Show show = get(showId);
        List<Seat> screenSeats = getScreenSeats(show);

        Set<String> bookedNumbers = bookedSeatRepository.findByShowId(showId).stream()
                .map(BookedSeat::getSeatId)
                .collect(Collectors.toSet());
        Set<String> heldNumbers = seatLockProvider.heldSeatIds(showId);

        return screenSeats.stream()
                .filter(seat -> !bookedNumbers.contains(seat.getNumber()))
                .filter(seat -> !heldNumbers.contains(seat.getNumber()))
                .toList();
    }
}
