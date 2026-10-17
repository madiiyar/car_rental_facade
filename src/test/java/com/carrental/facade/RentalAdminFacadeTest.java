package com.carrental.facade;

import com.carrental.coverage.DepositStatus;
import com.carrental.fleet.ReservationStatus;
import com.carrental.handover.GpsUnavailableException;
import com.carrental.handover.HandoverStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RentalAdminFacadeTest {

    @TempDir
    Path tempDir;

    private RentalScenario world;

    @BeforeEach
    void setUp() {
        world = RentalScenario.in(tempDir)
                .aCarIsAvailable("car-1")
                .customerHasCardLimit("cust-1", 1000);
    }

    // ---------- перезагрузка хранилища посреди цепочки ----------

    @Test
    void afterRestart_chainStuckAtPendingC_isCompensatedFromPersistedStatus() {
        RentalChain stuck = world.chainStuckAfterDeposit("cust-1", "car-1");

        RentalScenario restarted = world.afterRestart();   // вся память потеряна, остались только файлы
        assertEquals(RentalChainStatus.PENDING_C, restarted.chainOnDisk(stuck.getId()).getStatus());
        restarted.admin.recoverStuckChains();

        RentalChain after = restarted.chainOnDisk(stuck.getId());
        assertEquals(RentalChainStatus.COMPENSATED, after.getStatus());
        assertEquals(DepositStatus.RELEASED, restarted.depositOf(after));
        assertEquals(ReservationStatus.RELEASED, restarted.reservationOf(after));
    }

    @Test
    void afterRestart_chainStuckAtPendingB_releasesOnlyTheReservation() {
        RentalChain stuck = world.chainStuckAfterReservation("cust-1", "car-1");

        RentalScenario restarted = world.afterRestart();
        restarted.admin.recoverStuckChains();

        RentalChain after = restarted.chainOnDisk(stuck.getId());
        assertEquals(RentalChainStatus.COMPENSATED, after.getStatus());
        assertEquals(ReservationStatus.RELEASED, restarted.reservationOf(after));
        assertTrue(restarted.coverage.all().isEmpty());
    }

    @Test
    void recoveryLeavesCompletedChainsAlone() {
        RentalChain done = world.rent("cust-1", "car-1");

        world.afterRestart().admin.recoverStuckChains();

        assertEquals(RentalChainStatus.COMPLETED, world.chainOnDisk(done.getId()).getStatus());
    }

    // ---------- ручная компенсация ----------

    @Test
    void manualCompensationRollsBackAStuckChain() {
        RentalChain stuck = world.chainStuckAfterDeposit("cust-1", "car-1");

        world.admin.compensate(stuck.getId());

        assertEquals(RentalChainStatus.COMPENSATED, world.chainOnDisk(stuck.getId()).getStatus());
    }

    @Test
    void compensationSkipsStepsThatWereAlreadyUndone() {
        RentalChain stuck = world.chainStuckAfterDeposit("cust-1", "car-1");
        world.coverage.release(stuck.getDepositId());   // имитация падения посреди отката: B уже снят

        world.admin.compensate(stuck.getId());          // не должно упасть на повторном release

        RentalChain after = world.chainOnDisk(stuck.getId());
        assertEquals(RentalChainStatus.COMPENSATED, after.getStatus());
        assertEquals(ReservationStatus.RELEASED, world.reservationOf(after));
    }

    @Test
    void completedChainCannotBeCompensated() {
        RentalChain done = world.rent("cust-1", "car-1");

        assertThrows(ChainNotCompensableException.class, () -> world.admin.compensate(done.getId()));
    }

    @Test
    void compensatingAnUnknownChainIsRejected() {
        assertThrows(RentalChainNotFoundException.class, () -> world.admin.compensate("CH-nope"));
    }

    // ---------- осмотр ----------

    @Test
    void inspectShowsRealStatusesOfAllThreeSubsystems() {
        RentalChain done = world.rent("cust-1", "car-1");

        ChainReport report = world.admin.inspect(done.getId());

        assertEquals(RentalChainStatus.COMPLETED, report.chain().getStatus());
        assertEquals(ReservationStatus.CONFIRMED, report.reservation());
        assertEquals(DepositStatus.AUTHORIZED, report.deposit());
        assertEquals(HandoverStatus.ACTIVE, report.handover());
    }

    @Test
    void inspectShowsTheCancelledHandoverOfAFailedChain() {
        world.carGpsIsOffline("car-1");
        world.rentingFailsWith(GpsUnavailableException.class, "cust-1", "car-1");

        ChainReport report = world.admin.inspect(world.theOnlyChain().getId());

        assertEquals(RentalChainStatus.COMPENSATED, report.chain().getStatus());
        assertEquals(ReservationStatus.RELEASED, report.reservation());
        assertEquals(DepositStatus.RELEASED, report.deposit());
        assertEquals(HandoverStatus.CANCELLED, report.handover());
    }

    @Test
    void listChainsReturnsEveryChainOnDisk() {
        world.rent("cust-1", "car-1");
        assertEquals(1, world.admin.listChains().size());
    }
}