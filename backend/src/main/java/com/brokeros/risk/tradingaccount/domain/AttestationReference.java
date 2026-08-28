package com.brokeros.risk.tradingaccount.domain;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Pattern;

public record AttestationReference(String source, String reference) {

    private static final Pattern SOURCE = Pattern.compile("[a-z][a-z0-9-]{0,31}");

    public AttestationReference {
        Objects.requireNonNull(source, "attestation source must not be null");
        Objects.requireNonNull(reference, "attestation reference must not be null");
        if (!SOURCE.matcher(source).matches() || !isSafe(reference, 128, 512)) {
            throw new IllegalArgumentException("attestation is invalid");
        }
    }

    static boolean isSafe(String value, int maxCodePoints, int maxBytes) {
        int first = value.isEmpty() ? -1 : value.codePointAt(0);
        int last = value.isEmpty() ? -1 : value.codePointBefore(value.length());
        if (value.isEmpty()
                || Character.isWhitespace(first) || Character.isSpaceChar(first)
                || Character.isWhitespace(last) || Character.isSpaceChar(last)
                || value.codePointCount(0, value.length()) > maxCodePoints
                || value.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            return false;
        }
        for (int index = 0; index < value.length();) {
            char character = value.charAt(index);
            if (Character.isSurrogate(character)
                    && (!Character.isHighSurrogate(character)
                    || index + 1 >= value.length()
                    || !Character.isLowSurrogate(value.charAt(index + 1)))) {
                return false;
            }
            int codePoint = value.codePointAt(index);
            if (codePoint == 0 || Character.isISOControl(codePoint)) return false;
            index += Character.charCount(codePoint);
        }
        return true;
    }
}
