package com.carrental.fleet;

public enum ReservationStatus {
    HELD, CONFIRMED, RELEASED;

    public boolean canTransitionTo(ReservationStatus target) {
        return switch (this) {
            case HELD -> target == CONFIRMED || target == RELEASED;
            case CONFIRMED -> target == RELEASED;
            case RELEASED -> false;   // конечное состояние
        };
    }
}