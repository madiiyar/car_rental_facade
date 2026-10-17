package com.carrental.facade;

import com.carrental.coverage.Deposit;
import com.carrental.coverage.DepositHandler;
import com.carrental.coverage.InsurancePlan;
import com.carrental.fleet.FleetAvailabilityTracker;
import com.carrental.fleet.Reservation;
import com.carrental.handover.HandoverService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RentalFacade {

    private final FleetAvailabilityTracker fleet;
    private final DepositHandler coverage;
    private final HandoverService handover;
    private final RentalChainRepository chains;
    private final ChainCompensator compensator;

    public RentalFacade(FleetAvailabilityTracker fleet, DepositHandler coverage,
                        HandoverService handover, RentalChainRepository chains) {
        this.fleet = fleet;
        this.coverage = coverage;
        this.handover = handover;
        this.chains = chains;
        this.compensator = new ChainCompensator(fleet, coverage, chains);
    }

    /** Одна логическая операция для клиента: A -> B -> C. При сбое откатывает уже сделанное и пробрасывает исходную ошибку. */
    public RentalChain rentCar(String customerId, String carId, LocalDate start, LocalDate end, InsurancePlan plan) {
        // Шаг A. Если он бросит исключение, нигде ничего не закоммичено, откатывать нечего.
        Reservation reservation = fleet.reserve(carId, customerId, start, end);
        RentalChain chain = RentalChain.begin(customerId, carId, reservation.getId());
        chains.save(chain);                                           // на диске: PENDING_B

        try {
            // Шаг B.
            long days = ChronoUnit.DAYS.between(reservation.getStartDate(), reservation.getEndDate());
            Deposit deposit = coverage.authorize(reservation.getId(), customerId, plan, days);
            chain.depositAuthorized(deposit.getId());
            chains.save(chain);                                       // на диске: PENDING_C

            // Шаг C.
            handover.startHandover(reservation.getId(), carId);

            // Всё прошло: подтверждаем бронь и закрываем цепочку.
            fleet.confirm(reservation.getId());
            chain.completed();
            chains.save(chain);                                       // на диске: COMPLETED
            return chain;
        } catch (RuntimeException failure) {
            // Какую часть откатывать, решает ChainCompensator по записи цепочки на диске.
            try {
                compensator.compensate(chain.getId());
            } catch (RuntimeException compensationFailure) {
                // Откат тоже упал: цепочка остаётся PENDING_* и её подберёт RentalAdminFacade.
                failure.addSuppressed(compensationFailure);
            }
            throw failure;
        }
    }
}