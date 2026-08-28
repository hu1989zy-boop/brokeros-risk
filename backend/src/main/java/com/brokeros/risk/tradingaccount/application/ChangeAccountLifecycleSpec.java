package com.brokeros.risk.tradingaccount.application;

import java.util.Objects;

public record ChangeAccountLifecycleSpec(AuthorityOperationRequest request) {
    public ChangeAccountLifecycleSpec {
        Objects.requireNonNull(request, "request must not be null");
        if (request.operationType().isScopeOperation() || request.operationType().isRegistration()) {
            throw new IllegalArgumentException("request is not an account lifecycle change");
        }
    }
}
