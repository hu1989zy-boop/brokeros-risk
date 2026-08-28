package com.brokeros.risk.tradingaccount.application;

import java.util.Objects;

public record ChangeScopeLifecycleSpec(AuthorityOperationRequest request) {
    public ChangeScopeLifecycleSpec {
        Objects.requireNonNull(request, "request must not be null");
        if (!request.operationType().isScopeOperation() || request.operationType().isRegistration()) {
            throw new IllegalArgumentException("request is not a scope lifecycle change");
        }
    }
}
