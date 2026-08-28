package com.brokeros.risk.tradingaccount.domain;

import java.util.Objects;

public record ExternalAccountIdentity(
        AccountAuthorityScopeRef scopeRef,
        SourceNamespace namespace,
        ExternalAccountKey externalAccountKey) {

    public ExternalAccountIdentity {
        Objects.requireNonNull(scopeRef, "scopeRef must not be null");
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(externalAccountKey, "externalAccountKey must not be null");
    }
}
