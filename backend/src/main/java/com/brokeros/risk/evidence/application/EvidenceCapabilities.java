package com.brokeros.risk.evidence.application;

import com.brokeros.risk.security.domain.Capability;

public final class EvidenceCapabilities {

    public static final Capability RECORD = new Capability("evidence:record");
    public static final Capability CORRECT = new Capability("evidence:correct");
    public static final Capability READ = new Capability("evidence:read");

    private EvidenceCapabilities() {
    }
}
