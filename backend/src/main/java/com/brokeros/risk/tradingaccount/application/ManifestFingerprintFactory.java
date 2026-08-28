package com.brokeros.risk.tradingaccount.application;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.brokeros.risk.tradingaccount.domain.ManifestFingerprint;

public final class ManifestFingerprintFactory {

    private static final String DOMAIN = "brokeros-risk:q010:manifest-fingerprint:v1";

    public ManifestFingerprint create(AuthorityOperationRequest request) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream framed = new DataOutputStream(bytes)) {
                writeString(framed, DOMAIN);
                framed.writeLong(request.schemaVersion());
                writeString(framed, request.operationType().name());
                writeString(framed, request.scopeRef() == null ? null : request.scopeRef().value());
                writeString(framed, request.accountRef() == null ? null : request.accountRef().value());
                writeString(framed, request.namespace() == null ? null : request.namespace().sourceFamily());
                writeString(framed, request.namespace() == null ? null : request.namespace().sourceInstance());
                writeString(framed, request.namespace() == null ? null : request.namespace().sourceServer());
                writeString(framed, request.namespace() == null ? null : request.namespace().sourceEnvironment());
                writeBytes(framed, request.externalAccountKey() == null
                        ? null : request.externalAccountKey().utf8Bytes());
                if (request.expectedVersion() == null) {
                    framed.writeInt(-1);
                } else {
                    framed.writeInt(Long.BYTES);
                    framed.writeLong(request.expectedVersion());
                }
                writeString(framed, request.attestation().source());
                writeString(framed, request.attestation().reference());
                writeString(framed, request.reason().value());
                writeString(framed, request.changeReference().value());
            }
            return new ManifestFingerprint(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 fingerprint generation failed", exception);
        }
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        writeBytes(output, value == null ? null : value.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeBytes(DataOutputStream output, byte[] value) throws IOException {
        if (value == null) {
            output.writeInt(-1);
        } else {
            output.writeInt(value.length);
            output.write(value);
        }
    }
}
