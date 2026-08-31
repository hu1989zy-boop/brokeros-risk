package com.brokeros.risk.decision.domain;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public record ConclusionText(String value) {

    public ConclusionText {
        Objects.requireNonNull(value, "conclusionText must not be null");
        int bytes = value.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < 1 || bytes > 4000 || value.isBlank()) {
            throw new IllegalArgumentException(
                    "conclusionText must contain between 1 and 4000 UTF-8 bytes");
        }
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (codePoint == 0 || Character.isISOControl(codePoint)) {
                throw new IllegalArgumentException(
                        "conclusionText must not contain control characters");
            }
            offset += Character.charCount(codePoint);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
