package com.carrental.facade;

public class RentalChainNotFoundException extends RuntimeException {
    public RentalChainNotFoundException(String chainId) {
        super("Rental chain not found: " + chainId);
    }
}