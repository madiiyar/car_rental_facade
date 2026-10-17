package com.carrental.demo;

import com.carrental.coverage.*;
import com.carrental.infra.Database;

import java.nio.file.Path;

/** Подсистема B работает сама по себе, без фасада, A и C. */
public class CoverageDemo {
    public static void main(String[] args) {
        Database db = new Database(Path.of("data", "coverage-demo.db"));
        db.reset();
        DepositHandler handler = new DepositHandler(new DepositRepository(db), new InsuranceCalculator());
        handler.setCardLimit("cust-1", 500);

        Deposit d = handler.authorize("R-demo", "cust-1", InsurancePlan.BASIC, 3);
        System.out.println("Authorized: " + d.getId() + ", amount=" + d.getAmount() + ", " + d.getStatus());
        System.out.println("Available: " + handler.availableFunds("cust-1"));

        try {
            handler.authorize("R-demo2", "cust-1", InsurancePlan.FULL, 10);
        } catch (InsufficientFundsException e) {
            System.out.println("Expected failure: " + e.getMessage());
        }

        System.out.println("Released: " + handler.release(d.getId()).getStatus());
        System.out.println("Available: " + handler.availableFunds("cust-1"));
    }
}