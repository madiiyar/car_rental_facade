package com.carrental.fleet;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PricingEngine {

    public long daysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new InvalidRentalPeriodException(
                    "End date must be after start date (start=" + start + ", end=" + end + ")");
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    public long quote(Car car, LocalDate start, LocalDate end) {
        return daysBetween(start, end) * car.dailyRate();
    }
}