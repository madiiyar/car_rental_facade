package com.carrental.facade;

public enum RentalChainStatus {
    PENDING_B,    // A закоммичена, B ещё нет
    PENDING_C,    // A и B закоммичены, C ещё нет
    COMPLETED,    // вся цепочка прошла
    COMPENSATED   // цепочка откачена
}