package com.brokeros.risk.action.application;

import com.brokeros.risk.security.domain.Capability;

public final class ActionCapabilities {

    public static final Capability RECORD = new Capability("action:record");
    public static final Capability READ = new Capability("action:read");

    private ActionCapabilities() {
    }
}
