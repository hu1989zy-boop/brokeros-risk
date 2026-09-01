package com.brokeros.risk.actionoutcome.application;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.brokeros.risk.actionoutcome.domain.ActionOutcomeSemanticFingerprint;

public final class ActionOutcomeFingerprintFactory {

    private static final String RECORD_DOMAIN =
            "brokeros-risk:q014:record-fingerprint:v1";

    public ActionOutcomeSemanticFingerprint forRecord(
            String actionRef,
            String outcomeText) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream framed = new DataOutputStream(bytes)) {
                write(framed, RECORD_DOMAIN);
                write(framed, actionRef);
                write(framed, outcomeText);
            }
            return new ActionOutcomeSemanticFingerprint(
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
