package com.brokeros.risk.decision.application;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import com.brokeros.risk.decision.domain.DecisionSemanticFingerprint;

public final class DecisionFingerprintFactory {

    private static final String RECORD_DOMAIN =
            "brokeros-risk:q012:record-fingerprint:v1";

    public DecisionSemanticFingerprint forRecord(
            String subjectRef,
            List<String> evidenceRefs,
            String conclusionText) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream framed = new DataOutputStream(bytes)) {
                write(framed, RECORD_DOMAIN);
                write(framed, subjectRef);
                List<String> canonicalRefs = canonicalEvidenceRefs(evidenceRefs);
                framed.writeInt(canonicalRefs.size());
                for (String evidenceRef : canonicalRefs) {
                    write(framed, evidenceRef);
                }
                write(framed, conclusionText);
            }
            return new DecisionSemanticFingerprint(
                    MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 fingerprint generation failed", exception);
        }
    }

    private List<String> canonicalEvidenceRefs(List<String> evidenceRefs) {
        if (evidenceRefs == null) {
            return List.of();
        }
        TreeSet<String> sorted = new TreeSet<>(Comparator.nullsFirst(String::compareTo));
        sorted.addAll(evidenceRefs);
        return new ArrayList<>(sorted);
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
