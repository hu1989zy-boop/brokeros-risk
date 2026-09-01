package com.brokeros.risk.actionoutcome.domain;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public record OutcomeText(String value) {

    public OutcomeText {
        Objects.requireNonNull(value, "outcomeText must not be null");
        int bytes = value.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < 1 || bytes > 4000 || value.isBlank()) {
            throw new IllegalArgumentException(
                    "outcomeText must contain between 1 and 4000 UTF-8 bytes");
        }
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (codePoint == 0 || Character.isISOControl(codePoint)) {
                throw new IllegalArgumentException(
                        "outcomeText must not contain control characters");
            }
            offset += Character.charCount(codePoint);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
