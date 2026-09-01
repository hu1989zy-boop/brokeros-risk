package com.brokeros.risk.action.application;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.brokeros.risk.action.domain.ActionSemanticFingerprint;

public final class ActionFingerprintFactory {

    private static final String RECORD_DOMAIN =
            "brokeros-risk:q013:record-fingerprint:v1";

    public ActionSemanticFingerprint forRecord(
            String decisionRef,
            String intentText) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream framed = new DataOutputStream(bytes)) {
                write(framed, RECORD_DOMAIN);
                write(framed, decisionRef);
                write(framed, intentText);
            }
            return new ActionSemanticFingerprint(
                    MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 fingerprint generation failed", exception);
        }
    }

    private void write(DataOutputStream framed, String value) throws IOException {
        if (value == null) {
            framed.writeInt(-1);
            return;
        }
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        framed.writeInt(encoded.length);
        framed.write(encoded);
    }
}
