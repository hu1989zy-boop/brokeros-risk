package com.brokeros.risk.evidence.application;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.brokeros.risk.evidence.domain.EvidenceFingerprint;

public final class EvidenceFingerprintFactory {

    private static final String RECORD_DOMAIN =
            "brokeros-risk:q011:record-fingerprint:v1";
    private static final String CORRECT_DOMAIN =
            "brokeros-risk:q011:correct-fingerprint:v1";

    public EvidenceFingerprint forRecord(String subjectRef, String observationText) {
        return fingerprint(RECORD_DOMAIN, subjectRef, observationText);
    }

    public EvidenceFingerprint forCorrection(
            String targetEvidenceRef,
            String correctionReason,
            String observationText) {
        return fingerprint(
                CORRECT_DOMAIN, targetEvidenceRef, correctionReason, observationText);
    }

    private EvidenceFingerprint fingerprint(String... values) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream framed = new DataOutputStream(bytes)) {
                for (String value : values) {
                    byte[] encoded = value == null ? null : value.getBytes(StandardCharsets.UTF_8);
                    if (encoded == null) {
                        framed.writeInt(-1);
                    } else {
                        framed.writeInt(encoded.length);
                        framed.write(encoded);
                    }
                }
            }
            return new EvidenceFingerprint(
                    MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 fingerprint generation failed", exception);
        }
    }
}
