package com.brokeros.risk.riskcase.domain;

import java.util.Objects;

public final class RiskCaseText {

    private RiskCaseText() {
    }

    public static String require(String value, int maximumLength, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank() || value.length() > maximumLength) {
            throw new IllegalArgumentException(
                    name + " must be nonblank and at most " + maximumLength + " characters");
        }
        if (value.codePoints().anyMatch(codePoint -> codePoint == 0
                || (Character.isISOControl(codePoint)
                && codePoint != '\n' && codePoint != '\r' && codePoint != '\t'))) {
            throw new IllegalArgumentException(name + " contains a prohibited control character");
        }
        return value;
    }
}
