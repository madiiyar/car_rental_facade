package com.carrental.handover;

public class GpsTracker {

    private final HandoverRepository repository;

    public GpsTracker(HandoverRepository repository) {
        this.repository = repository;
    }

    /** Включить трекинг машины. Если устройство оффлайн, бросаем исключение. */
    public void activate(String carId) {
        if (!repository.isGpsOnline(carId)) {
            throw new GpsUnavailableException("GPS device of car " + carId + " is offline");
        }
    }

    /** Трекинг идёт, пока у машины есть активная выдача. */
    public boolean isTracking(String carId) {
        return repository.existsActiveHandover(carId);
    }
}