package com.brokeros.risk.riskcase.application;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class RiskCaseFingerprintFactory {

    public byte[] idempotencyKeyHash(String key) {
        if (key == null || key.length() < 16 || key.length() > 128
                || key.chars().anyMatch(character -> character < 0x21 || character > 0x7e)) {
            throw new RiskCaseException(
                    com.brokeros.risk.api.ResultCode.RISK_CASE_INVARIANT_VIOLATION);
        }
        return digest(key.getBytes(StandardCharsets.US_ASCII));
    }

    public byte[] requestHash(CreateRiskCaseCommand command) {
        MessageDigest digest = sha256();
        frame(digest, command.intakeSource());
        frame(digest, command.subjectType());
        frame(digest, command.subjectRef());
        frame(digest, command.intakeSummary());
        frame(digest, command.priority());
        frame(digest, command.decisionRef());
        return digest.digest();
    }

    private byte[] digest(byte[] value) {
        return sha256().digest(value);
    }

    private void frame(MessageDigest digest, String value) {
        if (value == null) {
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }
}
