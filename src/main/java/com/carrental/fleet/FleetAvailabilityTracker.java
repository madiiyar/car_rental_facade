package com.carrental.fleet;

import java.time.LocalDate;
import java.util.List;

public class FleetAvailabilityTracker {

    private final ReservationRepository repository;
    private final PricingEngine pricing;

    public FleetAvailabilityTracker(ReservationRepository repository, PricingEngine pricing) {
        this.repository = repository;
        this.pricing = pricing;
    }

    /** Шаг A цепочки: придержать машину на даты. Всё или ничего: если бросает исключение, ничего не записано. */
    public Reservation reserve(String carId, String customerId, LocalDate start, LocalDate end) {
        Car car = repository.findCar(carId)
                .orElseThrow(() -> new CarNotAvailableException("Unknown car: " + carId));
        long price = pricing.quote(car, start, end);   // заодно проверяет корректность периода
        if (repository.existsActiveOverlap(carId, start, end)) {
            throw new CarNotAvailableException(
                    "Car " + carId + " is already booked between " + start + " and " + end);
        }
        Reservation reservation = Reservation.hold(carId, customerId, start, end, price);
        repository.save(reservation);
        return reservation;
    }

    public boolean isAvailable(String carId, LocalDate start, LocalDate end) {
        return !repository.existsActiveOverlap(carId, start, end);
    }

    public Reservation confirm(String reservationId) {
        Reservation r = load(reservationId);
        r.confirm();
        repository.save(r);
        return r;
    }

    /** Компенсация шага A: освободить машину. */
    public Reservation release(String reservationId) {
        Reservation r = load(reservationId);
        r.release();
        repository.save(r);
        return r;
    }

    public Reservation get(String reservationId) {
        return load(reservationId);
    }

    public List<Reservation> all() {
        return repository.findAll();
    }

    public void addCar(Car car) {
        repository.saveCar(car);
    }

    public List<Car> cars() {
        return repository.findAllCars();
    }

    private Reservation load(String reservationId) {
        return repository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }
}