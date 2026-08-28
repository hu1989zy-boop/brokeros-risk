package com.brokeros.risk.tradingaccount.application;

import java.util.Objects;

import com.brokeros.risk.tradingaccount.domain.AccountAuthorityScopeRef;
import com.brokeros.risk.tradingaccount.domain.AttestationReference;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationId;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationType;
import com.brokeros.risk.tradingaccount.domain.ChangeReason;
import com.brokeros.risk.tradingaccount.domain.ChangeReference;
import com.brokeros.risk.tradingaccount.domain.ExternalAccountKey;
import com.brokeros.risk.tradingaccount.domain.SourceNamespace;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public record AuthorityOperationRequest(
        int schemaVersion,
        AuthorityOperationId operationId,
        AuthorityOperationType operationType,
        AccountAuthorityScopeRef scopeRef,
        TradingAccountRef accountRef,
        SourceNamespace namespace,
        ExternalAccountKey externalAccountKey,
        Long expectedVersion,
        AttestationReference attestation,
        ChangeReason reason,
        ChangeReference changeReference) {

    public AuthorityOperationRequest {
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("schema version must be exactly 1");
        }
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(operationType, "operationType must not be null");
        Objects.requireNonNull(attestation, "attestation must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(changeReference, "changeReference must not be null");
        if (expectedVersion != null && expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
        validateFieldMatrix(operationType, scopeRef, accountRef, namespace,
                externalAccountKey, expectedVersion);
    }

    private static void validateFieldMatrix(
            AuthorityOperationType type,
            AccountAuthorityScopeRef scopeRef,
            TradingAccountRef accountRef,
            SourceNamespace namespace,
            ExternalAccountKey externalAccountKey,
            Long expectedVersion) {
        boolean scopeRegistration = type == AuthorityOperationType.REGISTER_AUTHORITY_SCOPE;
        boolean accountRegistration = type == AuthorityOperationType.REGISTER_TRADING_ACCOUNT;
        boolean scopeLifecycle = type.isScopeOperation() && !scopeRegistration;
        boolean accountLifecycle = !type.isScopeOperation() && !accountRegistration;
        boolean valid = (scopeRegistration
                && scopeRef == null && accountRef == null && namespace == null
                && externalAccountKey == null && expectedVersion == null)
                || (accountRegistration
                && scopeRef != null && accountRef == null && namespace != null
                && externalAccountKey != null && expectedVersion == null)
                || (scopeLifecycle
                && scopeRef != null && accountRef == null && namespace == null
                && externalAccountKey == null && expectedVersion != null)
                || (accountLifecycle
                && scopeRef == null && accountRef != null && namespace == null
                && externalAccountKey == null && expectedVersion != null);
        if (!valid) {
            throw new IllegalArgumentException("operation field matrix is invalid");
        }
    }
}
