package com.brokeros.risk.action.domain;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public record IntentText(String value) {

    public IntentText {
        Objects.requireNonNull(value, "intentText must not be null");
        int bytes = value.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < 1 || bytes > 4000 || value.isBlank()) {
            throw new IllegalArgumentException(
                    "intentText must contain between 1 and 4000 UTF-8 bytes");
        }
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (codePoint == 0 || Character.isISOControl(codePoint)) {
                throw new IllegalArgumentException(
                        "intentText must not contain control characters");
            }
            offset += Character.charCount(codePoint);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
