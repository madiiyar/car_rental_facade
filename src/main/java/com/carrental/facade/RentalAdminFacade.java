package com.carrental.facade;

import com.carrental.coverage.DepositHandler;
import com.carrental.coverage.DepositStatus;
import com.carrental.fleet.FleetAvailabilityTracker;
import com.carrental.fleet.ReservationStatus;
import com.carrental.handover.Handover;
import com.carrental.handover.HandoverService;
import com.carrental.handover.HandoverStatus;
import com.carrental.fleet.Car;

import java.util.ArrayList;
import java.util.List;

public class RentalAdminFacade {

    private final FleetAvailabilityTracker fleet;
    private final DepositHandler coverage;
    private final HandoverService handover;
    private final RentalChainRepository chains;
    private final ChainCompensator compensator;

    public RentalAdminFacade(FleetAvailabilityTracker fleet, DepositHandler coverage,
                             HandoverService handover, RentalChainRepository chains) {
        this.fleet = fleet;
        this.coverage = coverage;
        this.handover = handover;
        this.chains = chains;
        this.compensator = new ChainCompensator(fleet, coverage, chains);
    }

    public List<RentalChain> listChains() {
        return chains.findAll();
    }

    /** Осмотр: реальные статусы всех трёх подсистем, прочитанные с диска. */
    public ChainReport inspect(String chainId) {
        RentalChain chain = chains.findById(chainId)
                .orElseThrow(() -> new RentalChainNotFoundException(chainId));

        ReservationStatus reservationStatus = fleet.get(chain.getReservationId()).getStatus();

        DepositStatus depositStatus = chain.getDepositId() == null
                ? null
                : coverage.get(chain.getDepositId()).getStatus();

        HandoverStatus handoverStatus = handover.all().stream()
                .filter(h -> h.getReservationId().equals(chain.getReservationId()))
                .map(Handover::getStatus)
                .findFirst()
                .orElse(null);

        return new ChainReport(chain, reservationStatus, depositStatus, handoverStatus);
    }

    /** Ручная компенсация одной цепочки (допустима только для PENDING_B и PENDING_C). */
    public RentalChain compensate(String chainId) {
        return compensator.compensate(chainId);
    }

    /** После рестарта: найти зависшие цепочки (PENDING_B, PENDING_C) на диске и откатить каждую. */
    public List<RentalChain> recoverStuckChains() {
        List<RentalChain> stuck = new ArrayList<>(chains.findByStatus(RentalChainStatus.PENDING_B));
        stuck.addAll(chains.findByStatus(RentalChainStatus.PENDING_C));

        List<RentalChain> recovered = new ArrayList<>();
        for (RentalChain chain : stuck) {
            recovered.add(compensator.compensate(chain.getId()));
        }
        return recovered;
    }

    // ---------- управление сценариями для демо ----------

    public List<Car> listCars() {
        return fleet.cars();
    }

    public void addCar(Car car) {
        fleet.addCar(car);
    }

    /** Маленький лимит -> шаг B упадёт. */
    public void setCardLimit(String customerId, long limit) {
        coverage.setCardLimit(customerId, limit);
    }

    /** Оценка выше 3 -> шаг C упадёт. */
    public void setDamageScore(String carId, int score) {
        handover.setDamageScore(carId, score);
    }

    /** Оффлайн -> шаг C упадёт после записи выдачи. */
    public void setGpsOnline(String carId, boolean online) {
        handover.setGpsOnline(carId, online);
    }



}