package com.carrental.coverage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InsuranceCalculatorTest {

    private final InsuranceCalculator calculator = new InsuranceCalculator();

    @Test
    void basicPlanCostsTenPerDay() {
        assertEquals(30, calculator.premium(InsurancePlan.BASIC, 3));
    }

    @Test
    void fullPlanCostsMoreButNeedsSmallerSecurityDeposit() {
        assertEquals(75, calculator.premium(InsurancePlan.FULL, 3));
        assertEquals(100, calculator.securityDeposit(InsurancePlan.FULL));
        assertEquals(200, calculator.securityDeposit(InsurancePlan.BASIC));
    }

    @Test
    void totalIsPremiumPlusSecurityDeposit() {
        assertEquals(230, calculator.totalToAuthorize(InsurancePlan.BASIC, 3));
    }

    @Test
    void zeroDaysIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> calculator.premium(InsurancePlan.BASIC, 0));
    }
}