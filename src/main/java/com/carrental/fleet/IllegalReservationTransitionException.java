package com.carrental.fleet;

public class IllegalReservationTransitionException extends RuntimeException {
    public IllegalReservationTransitionException(String id, ReservationStatus from, ReservationStatus to) {
        super("Reservation " + id + ": illegal transition " + from + " -> " + to);
    }
}