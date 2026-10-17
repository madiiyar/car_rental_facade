package com.carrental.handover;

import com.carrental.infra.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HandoverServiceTest {

    @TempDir
    Path tempDir;

    private Database db;
    private HandoverService service;

    private HandoverService newService() {
        HandoverRepository repo = new HandoverRepository(db);
        return new HandoverService(repo, new DamageInspector(), new GpsTracker(repo));
    }

    @BeforeEach
    void setUp() {
        db = new Database(tempDir.resolve("handover.db"));
        service = newService();
    }

    @Test
    void healthyCarIsHandedOverTrackedAndSurvivesRestart() {
        service.setDamageScore("car-1", 1);

        Handover h = service.startHandover("R-1", "car-1");

        assertEquals(HandoverStatus.ACTIVE, h.getStatus());
        assertTrue(service.isTracking("car-1"));

        // "рестарт": новые объекты на том же файле
        HandoverService restarted = newService();
        assertEquals(HandoverStatus.ACTIVE, restarted.get(h.getId()).getStatus());
        assertTrue(restarted.isTracking("car-1"));
    }

    @Test
    void damagedCarIsRejectedAndNothingIsStored() {
        service.setDamageScore("car-1", 8);

        assertThrows(DamageFoundException.class, () -> service.startHandover("R-1", "car-1"));

        assertTrue(service.all().isEmpty());
    }

    @Test
    void offlineGpsIsRejectedAndHandoverEndsCancelled() {
        service.setGpsOnline("car-1", false);

        assertThrows(GpsUnavailableException.class, () -> service.startHandover("R-1", "car-1"));

        List<Handover> all = service.all();
        assertEquals(1, all.size());
        assertEquals(HandoverStatus.CANCELLED, all.get(0).getStatus());
        assertFalse(service.isTracking("car-1"));
    }

    @Test
    void cancellingActiveHandoverStopsTracking() {
        Handover h = service.startHandover("R-1", "car-1");
        service.cancel(h.getId());
        assertFalse(service.isTracking("car-1"));
    }

    @Test
    void unknownHandoverIsRejected() {
        assertThrows(HandoverNotFoundException.class, () -> service.cancel("H-nope"));
    }
}