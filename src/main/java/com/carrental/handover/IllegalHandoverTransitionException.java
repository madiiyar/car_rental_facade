package com.carrental.handover;

public class IllegalHandoverTransitionException extends RuntimeException {
    public IllegalHandoverTransitionException(String id, HandoverStatus from, HandoverStatus to) {
        super("Handover " + id + ": illegal transition " + from + " -> " + to);
    }
}