package com.brokeros.risk.tradingaccount.application;

import java.util.Objects;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationType;

public record RegisterScopeSpec(AuthorityOperationRequest request) {
    public RegisterScopeSpec {
        Objects.requireNonNull(request, "request must not be null");
        if (request.operationType() != AuthorityOperationType.REGISTER_AUTHORITY_SCOPE) {
            throw new IllegalArgumentException("request is not scope registration");
        }
    }
}
