package com.carrental.handover;

public class GpsUnavailableException extends RuntimeException {
    public GpsUnavailableException(String message) {
        super(message);
    }
}