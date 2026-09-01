package com.brokeros.risk.actionoutcome.application;

import com.brokeros.risk.security.domain.Capability;

public final class ActionOutcomeCapabilities {

    public static final Capability RECORD = new Capability("action-outcome:record");
    public static final Capability READ = new Capability("action-outcome:read");

    private ActionOutcomeCapabilities() {
    }
}
