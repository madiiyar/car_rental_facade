package com.carrental.facade;

import java.util.UUID;

public class RentalChain {

    private final String id;
    private final String customerId;
    private final String carId;
    private final String reservationId;
    private String depositId;              // пока шаг B не прошёл, пусто (null)
    private RentalChainStatus status;

    private RentalChain(String id, String customerId, String carId, String reservationId,
                        String depositId, RentalChainStatus status) {
        this.id = id;
        this.customerId = customerId;
        this.carId = carId;
        this.reservationId = reservationId;
        this.depositId = depositId;
        this.status = status;
    }

    /** Цепочка создаётся, когда шаг A уже закоммичен: ждёт шага B. */
    public static RentalChain begin(String customerId, String carId, String reservationId) {
        String id = "CH-" + UUID.randomUUID().toString().substring(0, 8);
        return new RentalChain(id, customerId, carId, reservationId, null, RentalChainStatus.PENDING_B);
    }

    public static RentalChain restore(String id, String customerId, String carId, String reservationId,
                                      String depositId, RentalChainStatus status) {
        return new RentalChain(id, customerId, carId, reservationId, depositId, status);
    }

    public void depositAuthorized(String depositId) {
        this.depositId = depositId;
        this.status = RentalChainStatus.PENDING_C;
    }

    public void completed() {
        this.status = RentalChainStatus.COMPLETED;
    }

    public void compensated() {
        this.status = RentalChainStatus.COMPENSATED;
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getCarId() { return carId; }
    public String getReservationId() { return reservationId; }
    public String getDepositId() { return depositId; }
    public RentalChainStatus getStatus() { return status; }
}