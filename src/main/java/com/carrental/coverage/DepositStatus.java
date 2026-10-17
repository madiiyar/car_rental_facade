package com.carrental.coverage;

public enum DepositStatus {
    AUTHORIZED, CAPTURED, RELEASED;

    public boolean canTransitionTo(DepositStatus target) {
        return switch (this) {
            case AUTHORIZED -> target == CAPTURED || target == RELEASED;
            case CAPTURED, RELEASED -> false;   // конечные состояния
        };
    }
}