package com.carrental.handover;

public class DamageInspector {

    static final int MAX_SCORE = 10;
    static final int MAX_ACCEPTABLE_SCORE = 3;

    /** Оценка повреждения 0..10: до 3 включительно машину можно выдавать. */
    public boolean isAcceptable(int damageScore) {
        if (damageScore < 0 || damageScore > MAX_SCORE) {
            throw new IllegalArgumentException("Damage score must be 0.." + MAX_SCORE + ": " + damageScore);
        }
        return damageScore <= MAX_ACCEPTABLE_SCORE;
    }
}