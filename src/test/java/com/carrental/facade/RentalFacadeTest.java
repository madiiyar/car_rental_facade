package com.carrental.facade;

import com.carrental.coverage.*;
import com.carrental.fleet.*;
import com.carrental.handover.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RentalFacadeTest {

    @TempDir
    Path tempDir;

    private RentalScenario world;

    @BeforeEach
    void setUp() {
        world = RentalScenario.in(tempDir)
                .aCarIsAvailable("car-1")
                .customerHasCardLimit("cust-1", 1000);
    }

    @Test
    void successfulRentalCompletesTheChainAndCommitsAllThreeSubsystems() {
        RentalChain chain = world.rent("cust-1", "car-1");

        RentalChain onDisk = world.chainOnDisk(chain.getId());
        assertEquals(RentalChainStatus.COMPLETED, onDisk.getStatus());
        assertEquals(ReservationStatus.CONFIRMED, world.reservationOf(onDisk));
        assertEquals(DepositStatus.AUTHORIZED, world.depositOf(onDisk));
        assertEquals(HandoverStatus.ACTIVE, world.theOnlyHandover().getStatus());
    }

    @Test
    void whenStepBFailsAfterAIsCommitted_onlyTheReservationIsCompensated() {
        world.customerHasCardLimit("cust-1", 100);   // на карте меньше, чем нужно (230)

        world.rentingFailsWith(InsufficientFundsException.class, "cust-1", "car-1");

        RentalChain chain = world.theOnlyChain();
        assertEquals(RentalChainStatus.COMPENSATED, chain.getStatus());
        assertEquals(ReservationStatus.RELEASED, world.reservationOf(chain));
        assertTrue(world.coverage.all().isEmpty());       // B ничего не успела записать
        assertTrue(world.handover.all().isEmpty());       // C даже не начинался
        assertTrue(world.fleet.isAvailable("car-1", RentalScenario.START, RentalScenario.END));
    }

    @Test
    void whenStepCFailsAfterAAndBAreCommitted_depositAndThenReservationAreCompensated() {
        world.carIsDamaged("car-1", 8);

        world.rentingFailsWith(DamageFoundException.class, "cust-1", "car-1");

        RentalChain chain = world.theOnlyChain();
        assertEquals(RentalChainStatus.COMPENSATED, chain.getStatus());
        assertEquals(DepositStatus.RELEASED, world.depositOf(chain));
        assertEquals(ReservationStatus.RELEASED, world.reservationOf(chain));
        assertEquals(1000, world.coverage.availableFunds("cust-1"));   // холд снят, лимит свободен
        assertTrue(world.handover.all().isEmpty());
    }

    @Test
    void whenGpsIsOffline_handoverEndsCancelledAndDepositAndReservationAreCompensated() {
        world.carGpsIsOffline("car-1");

        world.rentingFailsWith(GpsUnavailableException.class, "cust-1", "car-1");

        RentalChain chain = world.theOnlyChain();
        assertEquals(RentalChainStatus.COMPENSATED, chain.getStatus());
        assertEquals(DepositStatus.RELEASED, world.depositOf(chain));
        assertEquals(ReservationStatus.RELEASED, world.reservationOf(chain));
        assertEquals(HandoverStatus.CANCELLED, world.theOnlyHandover().getStatus());
    }

    @Test
    void whenTheCarIsNotAvailable_nothingIsCommittedAndNoChainIsCreated() {
        world.rent("cust-1", "car-1");   // первая аренда занимает машину

        world.rentingFailsWith(CarNotAvailableException.class, "cust-1", "car-1");

        assertEquals(1, world.chains.findAll().size());   // только первая цепочка
        assertEquals(1, world.coverage.all().size());
    }

    @Test
    void whenThePeriodIsInvalid_nothingIsCommitted() {
        assertThrows(InvalidRentalPeriodException.class,
                () -> world.facade.rentCar("cust-1", "car-1",
                        RentalScenario.END, RentalScenario.START, InsurancePlan.BASIC));

        assertTrue(world.chains.findAll().isEmpty());
        assertTrue(world.fleet.all().isEmpty());
        assertTrue(world.coverage.all().isEmpty());
    }
}