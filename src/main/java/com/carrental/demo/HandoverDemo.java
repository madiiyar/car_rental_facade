package com.carrental.demo;

import com.carrental.handover.*;
import com.carrental.infra.Database;

import java.nio.file.Path;

/** Подсистема C работает сама по себе, без фасада, A и B. */
public class HandoverDemo {
    public static void main(String[] args) {
        Database db = new Database(Path.of("data", "handover-demo.db"));
        db.reset();
        HandoverRepository repo = new HandoverRepository(db);
        HandoverService service = new HandoverService(repo, new DamageInspector(), new GpsTracker(repo));

        // 1. Всё хорошо
        service.setDamageScore("car-1", 1);
        Handover ok = service.startHandover("R-1", "car-1");
        System.out.println("Started: " + ok.getId() + ", " + ok.getStatus() + ", tracking=" + service.isTracking("car-1"));

        // 2. Найдено повреждение: на диске ничего не остаётся
        service.setDamageScore("car-2", 8);
        try {
            service.startHandover("R-2", "car-2");
        } catch (DamageFoundException e) {
            System.out.println("Expected failure: " + e.getMessage());
        }

        // 3. GPS оффлайн: C сама отменяет свою запись
        service.setGpsOnline("car-3", false);
        try {
            service.startHandover("R-3", "car-3");
        } catch (GpsUnavailableException e) {
            System.out.println("Expected failure: " + e.getMessage());
        }

        service.all().forEach(h -> System.out.println("  " + h.getId() + " car=" + h.getCarId() + " " + h.getStatus()));

        // 4. Отмена активной выдачи
        System.out.println("Cancelled: " + service.cancel(ok.getId()).getStatus()
                + ", tracking=" + service.isTracking("car-1"));
    }
}