package com.carrental.cli;

import com.carrental.coverage.InsurancePlan;
import com.carrental.facade.ChainReport;
import com.carrental.facade.RentalAdminFacade;
import com.carrental.facade.RentalChain;
import com.carrental.facade.RentalFacade;
import com.carrental.fleet.Car;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class ConsoleMenu {

    private final RentalFacade facade;
    private final RentalAdminFacade admin;
    private final Scanner in;

    public ConsoleMenu(RentalFacade facade, RentalAdminFacade admin, Scanner in) {
        this.facade = facade;
        this.admin = admin;
        this.in = in;
    }

    /** Возвращает true, если попросили сброс данных (Main удалит файлы и откроет приложение заново). */
    public boolean run() {
        while (true) {
            if (!in.hasNextLine()) {
                return false;   // ввод закрыт
            }
            printMenu();
            String choice = prompt("Choose");
            try {
                switch (choice) {
                    case "1" -> listCars();
                    case "2" -> rentCar();
                    case "3" -> listChains();
                    case "4" -> inspectChain();
                    case "5" -> compensateChain();
                    case "6" -> recoverStuckChains();
                    case "7" -> setCardLimit();
                    case "8" -> setDamageScore();
                    case "9" -> setGpsState();
                    case "10" -> {
                        return true;
                    }
                    case "0" -> {
                        return false;
                    }
                    default -> System.out.println("Unknown option: " + choice);
                }
            } catch (RuntimeException e) {
                System.out.println("ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("==== Car Rental (Facade demo) ====");
        System.out.println(" 1. List cars");
        System.out.println(" 2. Rent a car (runs A -> B -> C)");
        System.out.println(" 3. List rental chains");
        System.out.println(" 4. Inspect a chain (real statuses of A, B, C from disk)");
        System.out.println(" 5. Compensate a chain manually");
        System.out.println(" 6. Recover stuck chains");
        System.out.println(" -- failure controls --");
        System.out.println(" 7. Set customer card limit      (too low  -> step B fails)");
        System.out.println(" 8. Set car damage score 0..10   (above 3  -> step C fails)");
        System.out.println(" 9. Set car GPS online/offline   (offline  -> step C fails)");
        System.out.println("10. RESET all data");
        System.out.println(" 0. Exit");
    }

    // ---------- действия ----------

    private void listCars() {
        for (Car car : admin.listCars()) {
            System.out.printf("  %s  %-16s %-6s %d per day%n",
                    car.id(), car.model(), car.category(), car.dailyRate());
        }
    }

    private void rentCar() {
        String customerId = prompt("Customer id (e.g. cust-1)");
        String carId = prompt("Car id (e.g. car-1)");
        LocalDate start = LocalDate.parse(prompt("Start date (yyyy-mm-dd)"));
        LocalDate end = LocalDate.parse(prompt("End date (yyyy-mm-dd)"));
        InsurancePlan plan = InsurancePlan.valueOf(prompt("Insurance plan (BASIC/FULL)").toUpperCase());

        Set<String> chainsBefore = chainIds();
        try {
            RentalChain chain = facade.rentCar(customerId, carId, start, end, plan);
            System.out.println("SUCCESS: the rental is completed.");
            printReport(admin.inspect(chain.getId()));
        } catch (RuntimeException failure) {
            System.out.println("RENTAL FAILED: " + failure.getClass().getSimpleName() + ": " + failure.getMessage());
            List<RentalChain> created = admin.listChains().stream()
                    .filter(c -> !chainsBefore.contains(c.getId()))
                    .toList();
            if (created.isEmpty()) {
                System.out.println("No chain was created: nothing had been committed, nothing to compensate.");
            }
            for (RentalChain chain : created) {
                System.out.println("State on disk after the failure:");
                printReport(admin.inspect(chain.getId()));
            }
        }
    }

    private void listChains() {
        List<RentalChain> chains = admin.listChains();
        if (chains.isEmpty()) {
            System.out.println("  (no chains)");
        }
        for (RentalChain c : chains) {
            System.out.printf("  %s  %-12s customer=%s car=%s%n",
                    c.getId(), c.getStatus(), c.getCustomerId(), c.getCarId());
        }
    }

    private void inspectChain() {
        printReport(admin.inspect(prompt("Chain id")));
    }

    private void compensateChain() {
        RentalChain chain = admin.compensate(prompt("Chain id"));
        System.out.println("Compensated.");
        printReport(admin.inspect(chain.getId()));
    }

    private void recoverStuckChains() {
        List<RentalChain> recovered = admin.recoverStuckChains();
        System.out.println("Recovered " + recovered.size() + " stuck chain(s).");
        for (RentalChain chain : recovered) {
            printReport(admin.inspect(chain.getId()));
        }
    }

    private void setCardLimit() {
        String customerId = prompt("Customer id");
        long limit = Long.parseLong(prompt("New card limit"));
        admin.setCardLimit(customerId, limit);
        System.out.println("Card limit of " + customerId + " is now " + limit);
    }

    private void setDamageScore() {
        String carId = prompt("Car id");
        int score = Integer.parseInt(prompt("Damage score 0..10 (acceptable: up to 3)"));
        admin.setDamageScore(carId, score);
        System.out.println("Damage score of " + carId + " is now " + score);
    }

    private void setGpsState() {
        String carId = prompt("Car id");
        String state = prompt("GPS state (online/offline)");
        if (!state.equalsIgnoreCase("online") && !state.equalsIgnoreCase("offline")) {
            throw new IllegalArgumentException("Expected online or offline: " + state);
        }
        admin.setGpsOnline(carId, state.equalsIgnoreCase("online"));
        System.out.println("GPS of " + carId + " is now " + state.toLowerCase());
    }

    // ---------- вспомогательное ----------

    private Set<String> chainIds() {
        return admin.listChains().stream().map(RentalChain::getId).collect(Collectors.toSet());
    }

    private void printReport(ChainReport report) {
        RentalChain c = report.chain();
        System.out.printf("Chain %s [%s] customer=%s car=%s%n",
                c.getId(), c.getStatus(), c.getCustomerId(), c.getCarId());
        System.out.printf("  A reservation %s : %s%n", c.getReservationId(), orDash(report.reservation()));
        System.out.printf("  B deposit     %s : %s%n", orDash(c.getDepositId()), orDash(report.deposit()));
        System.out.printf("  C handover    %s : %s%n", "-", orDash(report.handover()));
    }

    private String orDash(Object value) {
        return value == null ? "-" : value.toString();
    }

    private String prompt(String label) {
        System.out.print(label + ": ");
        return in.nextLine().trim();
    }
}