package com.moviebooking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moviebooking.exception.SeatUnavailableException;

class SeatLockProviderTest {

    private static final String SHOW = "show-1";

    @Test
    void holdsSeatsAndReportsThemHeld() {
        SeatLockProvider provider = new SeatLockProvider(120);

        provider.hold(SHOW, List.of("A1", "A2"), "user-1");

        assertThat(provider.heldSeatIds(SHOW)).containsExactlyInAnyOrder("A1", "A2");
        assertThat(provider.isHeldBy(SHOW, "A1", "user-1")).isTrue();
        assertThat(provider.isHeldBy(SHOW, "A1", "user-2")).isFalse();
    }

    @Test
    void rejectsSeatAlreadyHeldByAnother() {
        SeatLockProvider provider = new SeatLockProvider(120);
        provider.hold(SHOW, List.of("A1"), "user-1");

        assertThatThrownBy(() -> provider.hold(SHOW, List.of("A1", "A2"), "user-2"))
                .isInstanceOf(SeatUnavailableException.class);

        // The failed hold is all-or-nothing: A2 must not have been taken.
        assertThat(provider.heldSeatIds(SHOW)).containsExactly("A1");
    }

    @Test
    void releaseFreesSeatsHeldByUser() {
        SeatLockProvider provider = new SeatLockProvider(120);
        provider.hold(SHOW, List.of("A1"), "user-1");

        // Another user cannot release someone else's hold.
        provider.release(SHOW, List.of("A1"), "user-2");
        assertThat(provider.heldSeatIds(SHOW)).containsExactly("A1");

        provider.release(SHOW, List.of("A1"), "user-1");
        assertThat(provider.heldSeatIds(SHOW)).isEmpty();
        assertThatCode(() -> provider.hold(SHOW, List.of("A1"), "user-2")).doesNotThrowAnyException();
    }

    @Test
    void expiredHoldFreesSeatLazily() throws InterruptedException {
        SeatLockProvider provider = new SeatLockProvider(1); // 1-second holds
        provider.hold(SHOW, List.of("A1"), "user-1");

        Thread.sleep(1100);

        assertThat(provider.heldSeatIds(SHOW)).isEmpty();
        assertThat(provider.isHeldBy(SHOW, "A1", "user-1")).isFalse();
        assertThatCode(() -> provider.hold(SHOW, List.of("A1"), "user-2")).doesNotThrowAnyException();
    }
}
