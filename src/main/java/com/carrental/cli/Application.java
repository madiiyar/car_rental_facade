package com.carrental.cli;

import com.carrental.coverage.DepositHandler;
import com.carrental.coverage.DepositRepository;
import com.carrental.coverage.InsuranceCalculator;
import com.carrental.facade.RentalAdminFacade;
import com.carrental.facade.RentalChainRepository;
import com.carrental.facade.RentalFacade;
import com.carrental.fleet.Car;
import com.carrental.fleet.FleetAvailabilityTracker;
import com.carrental.fleet.PricingEngine;
import com.carrental.fleet.ReservationRepository;
import com.carrental.handover.DamageInspector;
import com.carrental.handover.GpsTracker;
import com.carrental.handover.HandoverRepository;
import com.carrental.handover.HandoverService;
import com.carrental.infra.Database;

import java.nio.file.Path;
import java.util.List;

/** Composition root: единственное место, которое знает все классы и собирает из них систему. */
public final class Application {

    private static final List<String> DATABASE_FILES =
            List.of("fleet.db", "coverage.db", "handover.db", "chains.db");

    private final RentalFacade facade;
    private final RentalAdminFacade admin;

    private Application(RentalFacade facade, RentalAdminFacade admin) {
        this.facade = facade;
        this.admin = admin;
    }

    public static Application open(Path dataDir) {
        // У каждой подсистемы свой файл БД, плюс отдельный файл для цепочек.
        ReservationRepository fleetRepository =
                new ReservationRepository(new Database(dataDir.resolve("fleet.db")));
        FleetAvailabilityTracker fleet = new FleetAvailabilityTracker(fleetRepository, new PricingEngine());

        DepositHandler coverage = new DepositHandler(
                new DepositRepository(new Database(dataDir.resolve("coverage.db"))), new InsuranceCalculator());

        HandoverRepository handoverRepository =
                new HandoverRepository(new Database(dataDir.resolve("handover.db")));
        HandoverService handover =
                new HandoverService(handoverRepository, new DamageInspector(), new GpsTracker(handoverRepository));

        RentalChainRepository chains = new RentalChainRepository(new Database(dataDir.resolve("chains.db")));

        RentalFacade facade = new RentalFacade(fleet, coverage, handover, chains);
        RentalAdminFacade admin = new RentalAdminFacade(fleet, coverage, handover, chains);

        Application app = new Application(facade, admin);
        app.seedIfEmpty();
        return app;
    }

    /** Сброс к чистому состоянию: удаляем все четыре файла БД. */
    public static void reset(Path dataDir) {
        for (String file : DATABASE_FILES) {
            new Database(dataDir.resolve(file)).reset();
        }
    }

    /** Стартовые данные, чтобы демо можно было запустить сразу. */
    private void seedIfEmpty() {
        if (!admin.listCars().isEmpty()) {
            return;
        }
        admin.addCar(new Car("car-1", "Toyota Camry", "SEDAN", 50));
        admin.addCar(new Car("car-2", "Hyundai Tucson", "SUV", 80));
        admin.addCar(new Car("car-3", "BMW 3 Series", "SEDAN", 120));
        admin.setCardLimit("cust-1", 1000);   // хватает на любую аренду
        admin.setCardLimit("cust-2", 150);    // мало: шаг B упадёт
    }

    public RentalFacade facade() {
        return facade;
    }

    public RentalAdminFacade admin() {
        return admin;
    }
}