package com.ledgercore.domain;

public enum Currency {
    INR(2),
    USD(2),
    JPY(0),
    BHD(3);

    private final int minorUnits;

    Currency(int minorUnits) {
        this.minorUnits = minorUnits;
    }

    public int minorUnits() {
        return minorUnits;
    }
}
