package com.brokeros.risk.riskcase.domain;

public record ResolutionCycleNumber(int value) {

    public ResolutionCycleNumber {
        if (value < 1) {
            throw new IllegalArgumentException("resolution cycle must be positive");
        }
    }

    public ResolutionCycleNumber next() {
        return new ResolutionCycleNumber(Math.addExact(value, 1));
    }
}
