package com.brokeros.risk.decision.application;

import com.brokeros.risk.security.domain.Capability;

public final class DecisionCapabilities {

    public static final Capability RECORD = new Capability("decision:record");
    public static final Capability READ = new Capability("decision:read");

    private DecisionCapabilities() {
    }
}
