package com.brokeros.risk.riskcase.application;

import com.brokeros.risk.security.domain.Capability;

public final class RiskCaseCapabilities {

    public static final Capability CREATE = new Capability("risk-case:create");
    public static final Capability READ = new Capability("risk-case:read");
    public static final Capability ASSIGN = new Capability("risk-case:assign");
    public static final Capability ASSOCIATE = new Capability("risk-case:associate");
    public static final Capability REVIEW = new Capability("risk-case:review");
    public static final Capability RESOLVE = new Capability("risk-case:resolve");
    public static final Capability CLOSE = new Capability("risk-case:close");
    public static final Capability CANCEL = new Capability("risk-case:cancel");
    public static final Capability REOPEN = new Capability("risk-case:reopen");
    public static final Capability NOTE = new Capability("risk-case:note");

    private RiskCaseCapabilities() {
    }
}
