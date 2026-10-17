package com.carrental.coverage;

import java.util.UUID;

public class Deposit {

    private final String id;
    private final String reservationId;   // просто строка-ссылка, B ничего не знает про подсистему A
    private final String customerId;
    private final long amount;
    private DepositStatus status;

    private Deposit(String id, String reservationId, String customerId, long amount, DepositStatus status) {
        this.id = id;
        this.reservationId = reservationId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
    }

    /** Новый холд: всегда начинается со статуса AUTHORIZED. */
    public static Deposit authorize(String reservationId, String customerId, long amount) {
        String id = "D-" + UUID.randomUUID().toString().substring(0, 8);
        return new Deposit(id, reservationId, customerId, amount, DepositStatus.AUTHORIZED);
    }

    /** Восстановление из БД. */
    public static Deposit restore(String id, String reservationId, String customerId,
                                  long amount, DepositStatus status) {
        return new Deposit(id, reservationId, customerId, amount, status);
    }

    public void capture() {
        transitionTo(DepositStatus.CAPTURED);
    }

    public void release() {
        transitionTo(DepositStatus.RELEASED);
    }

    private void transitionTo(DepositStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalDepositTransitionException(id, status, target);
        }
        status = target;
    }

    public String getId() { return id; }
    public String getReservationId() { return reservationId; }
    public String getCustomerId() { return customerId; }
    public long getAmount() { return amount; }
    public DepositStatus getStatus() { return status; }
}