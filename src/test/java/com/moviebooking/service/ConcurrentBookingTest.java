package com.moviebooking.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;

import com.moviebooking.domain.BookedSeat;
import com.moviebooking.domain.Booking;
import com.moviebooking.domain.BookingStatus;
import com.moviebooking.domain.Screen;
import com.moviebooking.domain.SeatCategory;
import com.moviebooking.domain.Show;
import com.moviebooking.domain.Theater;
import com.moviebooking.repository.BookedSeatRepository;
import com.moviebooking.repository.BookingRepository;
import com.moviebooking.web.dto.AddScreenRequest;
import com.moviebooking.web.dto.CreateMovieRequest;
import com.moviebooking.web.dto.CreateShowRequest;
import com.moviebooking.web.dto.CreateUserRequest;
import com.moviebooking.web.dto.SeatDto;

/**
 * The correctness centerpiece: many users racing for the same seat.
 *
 * <p>{@link #onlyOneBookingWinsTheContestedSeat()} drives the real hold → pay flow from N
 * threads released simultaneously and asserts exactly one booking is CONFIRMED, the rest
 * fail cleanly, and exactly one booked-seat row is persisted.
 *
 * <p>{@link #uniqueIndexIsTheLastLineOfDefense()} races the persistence layer directly to
 * show the unique {@code (showId, seatId)} index alone prevents a double-booking, even if
 * the in-memory hold were bypassed.
 */
@SpringBootTest
class ConcurrentBookingTest {

    private static final int THREADS = 20;

    @Autowired
    private MovieService movieService;
    @Autowired
    private TheaterService theaterService;
    @Autowired
    private ShowService showService;
    @Autowired
    private UserService userService;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookedSeatRepository bookedSeatRepository;

    @Test
    void onlyOneBookingWinsTheContestedSeat() throws Exception {
        String showId = createShowWithSingleSeat("A1");
        List<String> userIds = createUsers(THREADS);

        AtomicInteger confirmed = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        runSimultaneously(THREADS, i -> {
            String userId = userIds.get(i);
            try {
                Booking booking = bookingService.hold(showId, userId, List.of("A1"));
                paymentService.pay(booking.getId(), userId);
                confirmed.incrementAndGet();
            } catch (RuntimeException expectedForLosers) {
                failed.incrementAndGet();
            }
        });

        assertThat(confirmed.get()).isEqualTo(1);
        assertThat(failed.get()).isEqualTo(THREADS - 1);

        // Exactly one seat committed, and exactly one CONFIRMED booking for the show.
        assertThat(bookedSeatRepository.findByShowId(showId)).hasSize(1);
        assertThat(bookingRepository.findByShowId(showId).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count()).isEqualTo(1);
    }

    @Test
    void uniqueIndexIsTheLastLineOfDefense() throws Exception {
        String showId = "race-show-" + System.nanoTime();

        AtomicInteger inserted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        runSimultaneously(THREADS, i -> {
            BookedSeat row = BookedSeat.builder()
                    .showId(showId)
                    .seatId("A1")
                    .bookingId("booking-" + i)
                    .userId("user-" + i)
                    .createdAt(Instant.now())
                    .build();
            try {
                bookedSeatRepository.save(row);
                inserted.incrementAndGet();
            } catch (DuplicateKeyException duplicate) {
                rejected.incrementAndGet();
            }
        });

        assertThat(inserted.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(THREADS - 1);
        assertThat(bookedSeatRepository.findByShowId(showId)).hasSize(1);
    }

    /** Runs {@code count} tasks on separate threads, all released at the same instant. */
    private void runSimultaneously(int count, java.util.function.IntConsumer task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(count);
        try {
            for (int i = 0; i < count; i++) {
                int index = i;
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        task.accept(index);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            ready.await(10, TimeUnit.SECONDS); // all threads parked and ready
            start.countDown();                 // fire them together
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    private String createShowWithSingleSeat(String seatNumber) {
        String movieId = movieService.create(new CreateMovieRequest("Race", 120)).getId();
        Theater theater = theaterService.create("Arena", "Pune");
        Screen screen = theaterService.addScreen(theater.getId(),
                new AddScreenRequest("Audi 1", List.of(new SeatDto(seatNumber, "A", SeatCategory.GOLD))));
        Show show = showService.create(new CreateShowRequest(
                movieId, theater.getId(), screen.getId(), Instant.parse("2030-10-01T10:00:00Z"), 120));
        return show.getId();
    }

    private List<String> createUsers(int count) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String email = "racer-" + i + "-" + System.nanoTime() + "@x.com";
            ids.add(userService.create(new CreateUserRequest("Racer " + i, email)).getId());
        }
        return ids;
    }
}
