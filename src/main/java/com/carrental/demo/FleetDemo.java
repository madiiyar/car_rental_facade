package com.carrental.demo;

import com.carrental.fleet.*;
import com.carrental.infra.Database;

import java.nio.file.Path;
import java.time.LocalDate;

/** Подсистема A работает сама по себе, без фасада и без B/C. */
public class FleetDemo {
    public static void main(String[] args) {
        Database db = new Database(Path.of("data", "fleet-demo.db"));
        db.reset();
        ReservationRepository repo = new ReservationRepository(db);
        repo.saveCar(new Car("car-1", "Toyota Camry", "SEDAN", 50));
        FleetAvailabilityTracker tracker = new FleetAvailabilityTracker(repo, new PricingEngine());

        Reservation r = tracker.reserve("car-1", "cust-1",
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 23));
        System.out.println("Held: " + r.getId() + ", price=" + r.getTotalPrice() + ", " + r.getStatus());

        System.out.println("Confirmed: " + tracker.confirm(r.getId()).getStatus());

        try {
            tracker.reserve("car-1", "cust-2", LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 25));
        } catch (CarNotAvailableException e) {
            System.out.println("Expected failure: " + e.getMessage());
        }

        System.out.println("Released: " + tracker.release(r.getId()).getStatus());
    }
}