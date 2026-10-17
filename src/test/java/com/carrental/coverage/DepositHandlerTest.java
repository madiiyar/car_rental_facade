package com.carrental.coverage;

import com.carrental.infra.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DepositHandlerTest {

    @TempDir
    Path tempDir;

    private Database db;
    private DepositHandler handler;

    @BeforeEach
    void setUp() {
        db = new Database(tempDir.resolve("coverage.db"));
        handler = new DepositHandler(new DepositRepository(db), new InsuranceCalculator());
        handler.setCardLimit("cust-1", 500);
    }

    @Test
    void authorizeReducesAvailableFundsAndSurvivesRestart() {
        Deposit d = handler.authorize("R-1", "cust-1", InsurancePlan.BASIC, 3);

        assertEquals(230, d.getAmount());
        assertEquals(270, handler.availableFunds("cust-1"));

        // "рестарт": новые объекты на том же файле
        DepositHandler restarted = new DepositHandler(new DepositRepository(db), new InsuranceCalculator());
        assertEquals(DepositStatus.AUTHORIZED, restarted.get(d.getId()).getStatus());
        assertEquals(270, restarted.availableFunds("cust-1"));
    }

    @Test
    void insufficientFundsIsRejectedAndNothingIsStored() {
        handler.setCardLimit("cust-1", 100);
        assertThrows(InsufficientFundsException.class,
                () -> handler.authorize("R-1", "cust-1", InsurancePlan.BASIC, 3));
        assertTrue(handler.all().isEmpty());
    }

    @Test
    void customerWithoutCardHasNoFunds() {
        assertThrows(InsufficientFundsException.class,
                () -> handler.authorize("R-1", "cust-without-card", InsurancePlan.BASIC, 1));
    }

    @Test
    void releaseFreesTheFunds() {
        Deposit d = handler.authorize("R-1", "cust-1", InsurancePlan.BASIC, 3);
        handler.release(d.getId());
        assertEquals(500, handler.availableFunds("cust-1"));
    }

    @Test
    void unknownDepositIsRejected() {
        assertThrows(DepositNotFoundException.class, () -> handler.release("D-nope"));
    }
}