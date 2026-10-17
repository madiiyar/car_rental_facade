package com.carrental.fleet;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationTest {

    private Reservation newHeldReservation() {
        return Reservation.hold("car-1", "cust-1",
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 23), 150);
    }

    @Test
    void newReservationIsHeld() {
        assertEquals(ReservationStatus.HELD, newHeldReservation().getStatus());
    }

    @Test
    void heldReservationCanBeConfirmed() {
        Reservation r = newHeldReservation();
        r.confirm();
        assertEquals(ReservationStatus.CONFIRMED, r.getStatus());
    }

    @Test
    void confirmedReservationCanBeReleased() {
        Reservation r = newHeldReservation();
        r.confirm();
        r.release();
        assertEquals(ReservationStatus.RELEASED, r.getStatus());
    }

    @Test
    void releasedReservationCannotBeConfirmed() {
        Reservation r = newHeldReservation();
        r.release();
        assertThrows(IllegalReservationTransitionException.class, r::confirm);
    }

    @Test
    void releasedReservationCannotBeReleasedAgain() {
        Reservation r = newHeldReservation();
        r.release();
        assertThrows(IllegalReservationTransitionException.class, r::release);
    }
}