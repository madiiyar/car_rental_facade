package com.carrental.fleet;

public class InvalidRentalPeriodException extends RuntimeException {
    public InvalidRentalPeriodException(String message) {
        super(message);
    }
}