package com.carrental.coverage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DepositTest {

    private Deposit newDeposit() {
        return Deposit.authorize("R-1", "cust-1", 230);
    }

    @Test
    void newDepositIsAuthorized() {
        assertEquals(DepositStatus.AUTHORIZED, newDeposit().getStatus());
    }

    @Test
    void authorizedDepositCanBeCaptured() {
        Deposit d = newDeposit();
        d.capture();
        assertEquals(DepositStatus.CAPTURED, d.getStatus());
    }

    @Test
    void authorizedDepositCanBeReleased() {
        Deposit d = newDeposit();
        d.release();
        assertEquals(DepositStatus.RELEASED, d.getStatus());
    }

    @Test
    void releasedDepositCannotBeCaptured() {
        Deposit d = newDeposit();
        d.release();
        assertThrows(IllegalDepositTransitionException.class, d::capture);
    }

    @Test
    void capturedDepositCannotBeReleased() {
        Deposit d = newDeposit();
        d.capture();
        assertThrows(IllegalDepositTransitionException.class, d::release);
    }
}