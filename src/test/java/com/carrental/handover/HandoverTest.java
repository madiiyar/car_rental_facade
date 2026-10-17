package com.carrental.handover;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HandoverTest {

    private Handover newHandover() {
        return Handover.create("R-1", "car-1");
    }

    @Test
    void newHandoverIsPending() {
        assertEquals(HandoverStatus.PENDING, newHandover().getStatus());
    }

    @Test
    void pendingHandoverCanBeActivated() {
        Handover h = newHandover();
        h.activate();
        assertEquals(HandoverStatus.ACTIVE, h.getStatus());
    }

    @Test
    void pendingAndActiveHandoverCanBeCancelled() {
        Handover pending = newHandover();
        pending.cancel();
        assertEquals(HandoverStatus.CANCELLED, pending.getStatus());

        Handover active = newHandover();
        active.activate();
        active.cancel();
        assertEquals(HandoverStatus.CANCELLED, active.getStatus());
    }

    @Test
    void cancelledHandoverCannotBeActivated() {
        Handover h = newHandover();
        h.cancel();
        assertThrows(IllegalHandoverTransitionException.class, h::activate);
    }

    @Test
    void activeHandoverCannotBeActivatedAgain() {
        Handover h = newHandover();
        h.activate();
        assertThrows(IllegalHandoverTransitionException.class, h::activate);
    }
}