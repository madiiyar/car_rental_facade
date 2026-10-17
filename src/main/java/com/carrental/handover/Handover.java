package com.carrental.handover;

import java.util.UUID;

public class Handover {

    private final String id;
    private final String reservationId;   // строки-ссылки: C ничего не знает про A и B
    private final String carId;
    private HandoverStatus status;

    private Handover(String id, String reservationId, String carId, HandoverStatus status) {
        this.id = id;
        this.reservationId = reservationId;
        this.carId = carId;
        this.status = status;
    }

    /** Новая выдача: всегда начинается со статуса PENDING. */
    public static Handover create(String reservationId, String carId) {
        String id = "H-" + UUID.randomUUID().toString().substring(0, 8);
        return new Handover(id, reservationId, carId, HandoverStatus.PENDING);
    }

    /** Восстановление из БД. */
    public static Handover restore(String id, String reservationId, String carId, HandoverStatus status) {
        return new Handover(id, reservationId, carId, status);
    }

    public void activate() {
        transitionTo(HandoverStatus.ACTIVE);
    }

    public void cancel() {
        transitionTo(HandoverStatus.CANCELLED);
    }

    private void transitionTo(HandoverStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalHandoverTransitionException(id, status, target);
        }
        status = target;
    }

    public String getId() { return id; }
    public String getReservationId() { return reservationId; }
    public String getCarId() { return carId; }
    public HandoverStatus getStatus() { return status; }
}