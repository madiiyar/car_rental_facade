package com.carrental.facade;

import com.carrental.coverage.DepositStatus;
import com.carrental.fleet.ReservationStatus;
import com.carrental.handover.HandoverStatus;

/** Реальные статусы всех трёх подсистем для одной цепочки. null значит, что записи нет. */
public record ChainReport(RentalChain chain,
                          ReservationStatus reservation,
                          DepositStatus deposit,
                          HandoverStatus handover) {
}