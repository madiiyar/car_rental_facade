package com.carrental.handover;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DamageInspectorTest {

    private final DamageInspector inspector = new DamageInspector();

    @Test
    void lightDamageIsAcceptable() {
        assertTrue(inspector.isAcceptable(0));
        assertTrue(inspector.isAcceptable(3));
    }

    @Test
    void seriousDamageIsNotAcceptable() {
        assertFalse(inspector.isAcceptable(4));
        assertFalse(inspector.isAcceptable(10));
    }

    @Test
    void scoreOutsideScaleIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> inspector.isAcceptable(-1));
        assertThrows(IllegalArgumentException.class, () -> inspector.isAcceptable(11));
    }
}