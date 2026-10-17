package com.carrental.coverage;

public class DepositNotFoundException extends RuntimeException {
    public DepositNotFoundException(String depositId) {
        super("Deposit not found: " + depositId);
    }
}