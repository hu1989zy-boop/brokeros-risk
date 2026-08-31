package com.brokeros.risk.evidence.domain;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

final class EvidenceText {

    private EvidenceText() {
    }

    static String require(String value, int maxBytes, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        int bytes = value.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < 1 || bytes > maxBytes || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must contain between 1 and " + maxBytes + " UTF-8 bytes");
        }
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (codePoint == 0 || Character.isISOControl(codePoint)) {
                throw new IllegalArgumentException(field + " must not contain control characters");
            }
            offset += Character.charCount(codePoint);
        }
        return value;
    }
}
