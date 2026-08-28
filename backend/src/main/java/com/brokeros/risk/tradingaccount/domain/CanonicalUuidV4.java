package com.brokeros.risk.tradingaccount.domain;

import java.util.Locale;
import java.util.UUID;

final class CanonicalUuidV4 {

    private CanonicalUuidV4() {
    }

    static String require(String value, String prefix) {
        if (value == null || !value.startsWith(prefix)) {
            throw new IllegalArgumentException("reference has an invalid prefix");
        }
        String uuidText = value.substring(prefix.length());
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidText);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("reference must contain a UUID", exception);
        }
        String canonical = uuid.toString().toLowerCase(Locale.ROOT);
        if (uuid.version() != 4 || !uuidText.equals(canonical)) {
            throw new IllegalArgumentException("reference must contain a canonical lowercase UUIDv4");
        }
        return prefix + canonical;
    }

    static String requireOperationId(String value) {
        return require(value, "");
    }
}
