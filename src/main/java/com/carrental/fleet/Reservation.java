package com.carrental.fleet;

import java.time.LocalDate;
import java.util.UUID;

public class Reservation {

    private final String id;
    private final String carId;
    private final String customerId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final long totalPrice;
    private ReservationStatus status;

    private Reservation(String id, String carId, String customerId, LocalDate startDate,
                        LocalDate endDate, long totalPrice, ReservationStatus status) {
        this.id = id;
        this.carId = carId;
        this.customerId = customerId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    /** Новая бронь: всегда начинается со статуса HELD. */
    public static Reservation hold(String carId, String customerId, LocalDate start,
                                   LocalDate end, long totalPrice) {
        String id = "R-" + UUID.randomUUID().toString().substring(0, 8);
        return new Reservation(id, carId, customerId, start, end, totalPrice, ReservationStatus.HELD);
    }

    /** Восстановление из БД: статус берётся тот, что был сохранён. */
    public static Reservation restore(String id, String carId, String customerId, LocalDate start,
                                      LocalDate end, long totalPrice, ReservationStatus status) {
        return new Reservation(id, carId, customerId, start, end, totalPrice, status);
    }

    public void confirm() {
        transitionTo(ReservationStatus.CONFIRMED);
    }

    public void release() {
        transitionTo(ReservationStatus.RELEASED);
    }

    private void transitionTo(ReservationStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalReservationTransitionException(id, status, target);
        }
        status = target;
    }

    public String getId() { return id; }
    public String getCarId() { return carId; }
    public String getCustomerId() { return customerId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public long getTotalPrice() { return totalPrice; }
    public ReservationStatus getStatus() { return status; }
}