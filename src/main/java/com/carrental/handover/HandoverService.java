package com.carrental.handover;

import java.util.List;

public class HandoverService {

    private final HandoverRepository repository;
    private final DamageInspector inspector;
    private final GpsTracker gpsTracker;

    public HandoverService(HandoverRepository repository, DamageInspector inspector, GpsTracker gpsTracker) {
        this.repository = repository;
        this.inspector = inspector;
        this.gpsTracker = gpsTracker;
    }

    /** Шаг C цепочки: осмотр машины, запись выдачи, включение GPS. */
    public Handover startHandover(String reservationId, String carId) {
        // 1. Осмотр. Если плохо, бросаем ДО save: на диске ничего не остаётся.
        int damageScore = repository.getDamageScore(carId);
        if (!inspector.isAcceptable(damageScore)) {
            throw new DamageFoundException(
                    "Car " + carId + " has damage score " + damageScore + " and cannot be handed over");
        }

        // 2. Запись выдачи: PENDING.
        Handover handover = Handover.create(reservationId, carId);
        repository.save(handover);

        // 3. Включаем GPS. Если не вышло, C сама убирает за собой и пробрасывает ошибку.
        try {
            gpsTracker.activate(carId);
        } catch (GpsUnavailableException e) {
            handover.cancel();
            repository.save(handover);
            throw e;
        }
        handover.activate();
        repository.save(handover);
        return handover;
    }

    public Handover cancel(String handoverId) {
        Handover h = load(handoverId);
        h.cancel();
        repository.save(h);
        return h;
    }

    public void setDamageScore(String carId, int score) {
        repository.setDamageScore(carId, score);
    }

    public void setGpsOnline(String carId, boolean online) {
        repository.setGpsOnline(carId, online);
    }

    public boolean isTracking(String carId) {
        return gpsTracker.isTracking(carId);
    }

    public Handover get(String handoverId) {
        return load(handoverId);
    }

    public List<Handover> all() {
        return repository.findAll();
    }

    private Handover load(String handoverId) {
        return repository.findById(handoverId)
                .orElseThrow(() -> new HandoverNotFoundException(handoverId));
    }
}