package com.carrental.facade;

import com.carrental.coverage.DepositHandler;
import com.carrental.coverage.DepositStatus;
import com.carrental.fleet.FleetAvailabilityTracker;
import com.carrental.fleet.ReservationStatus;

/** Откат цепочки. Решение, что откатывать, принимается по записи цепочки НА ДИСКЕ. */
class ChainCompensator {

    private final FleetAvailabilityTracker fleet;
    private final DepositHandler coverage;
    private final RentalChainRepository chains;

    ChainCompensator(FleetAvailabilityTracker fleet, DepositHandler coverage, RentalChainRepository chains) {
        this.fleet = fleet;
        this.coverage = coverage;
        this.chains = chains;
    }

    RentalChain compensate(String chainId) {
        // Всегда читаем цепочку с диска: никаких флагов в памяти.
        RentalChain chain = chains.findById(chainId)
                .orElseThrow(() -> new RentalChainNotFoundException(chainId));

        switch (chain.getStatus()) {
            case PENDING_B -> releaseReservation(chain);           // закоммичена только A
            case PENDING_C -> {                                    // закоммичены A и B
                releaseDeposit(chain);                             // сначала B...
                releaseReservation(chain);                         // ...потом A
            }
            default -> throw new ChainNotCompensableException(chainId, chain.getStatus());
        }

        chain.compensated();
        chains.save(chain);
        return chain;
    }

    // Каждый шаг пропускается, если сущность уже откачена: откат можно безопасно повторить.
    private void releaseDeposit(RentalChain chain) {
        if (coverage.get(chain.getDepositId()).getStatus() != DepositStatus.RELEASED) {
            coverage.release(chain.getDepositId());
        }
    }

    private void releaseReservation(RentalChain chain) {
        if (fleet.get(chain.getReservationId()).getStatus() != ReservationStatus.RELEASED) {
            fleet.release(chain.getReservationId());
        }
    }
}