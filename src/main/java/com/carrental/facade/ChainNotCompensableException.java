package com.carrental.facade;

public class ChainNotCompensableException extends RuntimeException {
    public ChainNotCompensableException(String chainId, RentalChainStatus status) {
        super("Chain " + chainId + " is " + status + " and cannot be compensated");
    }
}