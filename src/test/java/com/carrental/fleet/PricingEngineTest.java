package com.carrental.fleet;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PricingEngineTest {

    private final PricingEngine pricing = new PricingEngine();
    private final Car car = new Car("car-1", "Toyota Camry", "SEDAN", 100);

    @Test
    void priceIsDaysTimesDailyRate() {
        long price = pricing.quote(car, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 23));
        assertEquals(300, price);
    }


    @Test
    void endDateMustBeAfterStartDate() {
        LocalDate day = LocalDate.of(2026, 10, 20);
        assertThrows(InvalidRentalPeriodException.class, () -> pricing.quote(car, day, day));
    }
}