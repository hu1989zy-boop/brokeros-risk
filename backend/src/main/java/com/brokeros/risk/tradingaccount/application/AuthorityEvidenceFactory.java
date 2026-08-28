package com.brokeros.risk.tradingaccount.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.brokeros.risk.tradingaccount.domain.AuthorityProvenanceRef;
import com.brokeros.risk.tradingaccount.domain.AuthoritySnapshotRef;

public final class AuthorityEvidenceFactory {

    public AuthoritySnapshotRef snapshot(EligibilityPersistenceView view) {
        return new AuthoritySnapshotRef("tasv1-" + digest("snapshot", view));
    }

    public AuthorityProvenanceRef provenance(EligibilityPersistenceView view) {
        return new AuthorityProvenanceRef("tapv1-" + digest("provenance", view));
    }

    private String digest(String purpose, EligibilityPersistenceView view) {
        String framed = "brokeros-risk:q010:" + purpose + ":v1\n"
                + view.tradingAccountRef().value() + "\n"
                + view.accountVersion() + "\n"
                + view.accountLastOperationId().value() + "\n"
                + view.scopeVersion() + "\n"
                + view.scopeLastOperationId().value();
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(framed.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 evidence generation failed", exception);
        }
    }
}
