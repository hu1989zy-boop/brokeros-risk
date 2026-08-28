package com.brokeros.risk.tradingaccount.domain;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class ExternalAccountKey {

    private final String value;

    public ExternalAccountKey(String value) {
        this.value = Objects.requireNonNull(value, "external account key must not be null");
        int codePoints = value.codePointCount(0, value.length());
        int utf8Bytes = value.getBytes(StandardCharsets.UTF_8).length;
        if (codePoints < 1 || codePoints > 128 || utf8Bytes > 512
                || hasEdgeWhitespace(value) || hasUnsafeCodePoint(value)) {
            throw new IllegalArgumentException("external account key is invalid");
        }
    }

    private static boolean hasEdgeWhitespace(String value) {
        if (value.isEmpty()) return false;
        int first = value.codePointAt(0);
        int last = value.codePointBefore(value.length());
        return Character.isWhitespace(first) || Character.isSpaceChar(first)
                || Character.isWhitespace(last) || Character.isSpaceChar(last);
    }

    private static boolean hasUnsafeCodePoint(String value) {
        for (int index = 0; index < value.length();) {
            char character = value.charAt(index);
            if (Character.isSurrogate(character)) {
                if (!Character.isHighSurrogate(character)
                        || index + 1 >= value.length()
                        || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    return true;
                }
            }
            int codePoint = value.codePointAt(index);
            if (codePoint == 0 || Character.isISOControl(codePoint)) {
                return true;
            }
            index += Character.charCount(codePoint);
        }
        return false;
    }

    public String value() {
        return value;
    }

    public byte[] utf8Bytes() {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ExternalAccountKey key && value.equals(key.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "ExternalAccountKey[REDACTED]";
    }
}
