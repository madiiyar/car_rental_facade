package com.carrental.handover;

public enum HandoverStatus {
    PENDING, ACTIVE, CANCELLED;

    public boolean canTransitionTo(HandoverStatus target) {
        return switch (this) {
            case PENDING -> target == ACTIVE || target == CANCELLED;
            case ACTIVE -> target == CANCELLED;
            case CANCELLED -> false;   // конечное состояние
        };
    }
}