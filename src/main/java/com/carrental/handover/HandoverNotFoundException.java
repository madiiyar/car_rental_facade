package com.carrental.handover;

public class HandoverNotFoundException extends RuntimeException {
    public HandoverNotFoundException(String handoverId) {
        super("Handover not found: " + handoverId);
    }
}