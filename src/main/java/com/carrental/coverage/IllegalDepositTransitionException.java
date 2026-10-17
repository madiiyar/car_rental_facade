package com.carrental.coverage;

public class IllegalDepositTransitionException extends RuntimeException {
    public IllegalDepositTransitionException(String id, DepositStatus from, DepositStatus to) {
        super("Deposit " + id + ": illegal transition " + from + " -> " + to);
    }
}