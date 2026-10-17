package com.carrental.facade;

import com.carrental.coverage.*;
import com.carrental.fleet.*;
import com.carrental.handover.*;
import com.carrental.infra.Database;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Тестовый язык: собирает всю систему на временных файлах и читается как фразы про аренду машины. */
class RentalScenario {

    static final LocalDate START = LocalDate.of(2026, 10, 20);
    static final LocalDate END = LocalDate.of(2026, 10, 23);   // 3 дня: BASIC = 30 страховка + 200 залог = 230

    private final Path dir;
    private final ReservationRepository fleetRepository;
    final FleetAvailabilityTracker fleet;
    final DepositHandler coverage;
    final HandoverService handover;
    final RentalChainRepository chains;
    final RentalFacade facade;
    final RentalAdminFacade admin;

    private RentalScenario(Path dir) {
        this.dir = dir;
        // У каждой подсистемы свой файл БД, плюс отдельный файл для цепочек.
        fleetRepository = new ReservationRepository(new Database(dir.resolve("fleet.db")));
        fleet = new FleetAvailabilityTracker(fleetRepository, new PricingEngine());
        coverage = new DepositHandler(
                new DepositRepository(new Database(dir.resolve("coverage.db"))), new InsuranceCalculator());
        HandoverRepository handoverRepository = new HandoverRepository(new Database(dir.resolve("handover.db")));
        handover = new HandoverService(handoverRepository, new DamageInspector(), new GpsTracker(handoverRepository));
        chains = new RentalChainRepository(new Database(dir.resolve("chains.db")));
        facade = new RentalFacade(fleet, coverage, handover, chains);
        admin = new RentalAdminFacade(fleet, coverage, handover, chains);
    }

    static RentalScenario in(Path dir) {
        return new RentalScenario(dir);
    }

    /** Имитация рестарта приложения: все объекты новые, файлы на диске те же. */
    RentalScenario afterRestart() {
        return new RentalScenario(dir);
    }

    // ---------- подготовка мира ----------

    RentalScenario aCarIsAvailable(String carId) {
        fleetRepository.saveCar(new Car(carId, "Toyota Camry", "SEDAN", 50));
        return this;
    }

    RentalScenario customerHasCardLimit(String customerId, long limit) {
        coverage.setCardLimit(customerId, limit);
        return this;
    }

    RentalScenario carIsDamaged(String carId, int damageScore) {
        handover.setDamageScore(carId, damageScore);
        return this;
    }

    RentalScenario carGpsIsOffline(String carId) {
        handover.setGpsOnline(carId, false);
        return this;
    }

    // ---------- действия ----------

    RentalChain rent(String customerId, String carId) {
        return facade.rentCar(customerId, carId, START, END, InsurancePlan.BASIC);
    }

    <T extends Throwable> T rentingFailsWith(Class<T> expected, String customerId, String carId) {
        return assertThrows(expected, () -> rent(customerId, carId));
    }

    /** Имитация падения приложения сразу после шага A: на диске бронь и цепочка PENDING_B. */
    RentalChain chainStuckAfterReservation(String customerId, String carId) {
        Reservation reservation = fleet.reserve(carId, customerId, START, END);
        RentalChain chain = RentalChain.begin(customerId, carId, reservation.getId());
        chains.save(chain);
        return chain;
    }

    /** Имитация падения сразу после шага B: на диске бронь, холд и цепочка PENDING_C. */
    RentalChain chainStuckAfterDeposit(String customerId, String carId) {
        Reservation reservation = fleet.reserve(carId, customerId, START, END);
        Deposit deposit = coverage.authorize(reservation.getId(), customerId, InsurancePlan.BASIC, 3);
        RentalChain chain = RentalChain.begin(customerId, carId, reservation.getId());
        chain.depositAuthorized(deposit.getId());
        chains.save(chain);
        return chain;
    }

    // ---------- чтение с диска ----------

    RentalChain chainOnDisk(String chainId) {
        return chains.findById(chainId).orElseThrow();
    }

    RentalChain theOnlyChain() {
        List<RentalChain> all = chains.findAll();
        assertEquals(1, all.size());
        return all.get(0);
    }

    Handover theOnlyHandover() {
        List<Handover> all = handover.all();
        assertEquals(1, all.size());
        return all.get(0);
    }

    ReservationStatus reservationOf(RentalChain chain) {
        return fleet.get(chain.getReservationId()).getStatus();
    }

    DepositStatus depositOf(RentalChain chain) {
        return coverage.get(chain.getDepositId()).getStatus();
    }
}