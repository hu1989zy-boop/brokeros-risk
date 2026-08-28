package com.brokeros.risk.tradingaccount.application;

import java.util.Objects;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationType;

public record RegisterAccountSpec(AuthorityOperationRequest request) {
    public RegisterAccountSpec {
        Objects.requireNonNull(request, "request must not be null");
        if (request.operationType() != AuthorityOperationType.REGISTER_TRADING_ACCOUNT) {
            throw new IllegalArgumentException("request is not account registration");
        }
    }
}
