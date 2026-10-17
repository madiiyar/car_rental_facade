package com.carrental.fleet;

import com.carrental.infra.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FleetAvailabilityTrackerTest {

    private static final LocalDate OCT_20 = LocalDate.of(2026, 10, 20);
    private static final LocalDate OCT_22 = LocalDate.of(2026, 10, 22);
    private static final LocalDate OCT_23 = LocalDate.of(2026, 10, 23);
    private static final LocalDate OCT_25 = LocalDate.of(2026, 10, 25);

    @TempDir
    Path tempDir;

    private Database db;
    private FleetAvailabilityTracker tracker;

    @BeforeEach
    void setUp() {
        db = new Database(tempDir.resolve("fleet.db"));
        ReservationRepository repository = new ReservationRepository(db);
        repository.saveCar(new Car("car-1", "Toyota Camry", "SEDAN", 50));
        tracker = new FleetAvailabilityTracker(repository, new PricingEngine());
    }

    @Test
    void reservationIsHeldAndPricedAndSurvivesRestart() {
        Reservation r = tracker.reserve("car-1", "cust-1", OCT_20, OCT_23);

        assertEquals(150, r.getTotalPrice());   // 3 дня * 50

        // "рестарт": новый репозиторий на том же файле
        Reservation fromDisk = new ReservationRepository(db).findById(r.getId()).orElseThrow();
        assertEquals(ReservationStatus.HELD, fromDisk.getStatus());
    }

    @Test
    void overlappingReservationIsRejected() {
        tracker.reserve("car-1", "cust-1", OCT_20, OCT_23);
        assertThrows(CarNotAvailableException.class,
                () -> tracker.reserve("car-1", "cust-2", OCT_22, OCT_25));
    }

    @Test
    void backToBackReservationsAreAllowed() {
        tracker.reserve("car-1", "cust-1", OCT_20, OCT_23);
        assertDoesNotThrow(() -> tracker.reserve("car-1", "cust-2", OCT_23, OCT_25));
    }

    @Test
    void releasedCarBecomesAvailableAgain() {
        Reservation r = tracker.reserve("car-1", "cust-1", OCT_20, OCT_23);
        tracker.release(r.getId());
        assertTrue(tracker.isAvailable("car-1", OCT_20, OCT_23));
    }

    @Test
    void unknownCarIsRejected() {
        assertThrows(CarNotAvailableException.class,
                () -> tracker.reserve("nope", "cust-1", OCT_20, OCT_23));
    }

    @Test
    void invalidPeriodIsRejectedAndNothingIsStored() {
        assertThrows(InvalidRentalPeriodException.class,
                () -> tracker.reserve("car-1", "cust-1", OCT_23, OCT_20));
        assertTrue(tracker.all().isEmpty());
    }
}