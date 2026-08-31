package com.brokeros.risk.decision.application;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.brokeros.risk.decision.domain.ConclusionText;
import com.brokeros.risk.decision.domain.DecisionOperationId;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public record RecordDecisionSpec(
        DecisionOperationId operationId,
        TradingAccountRef subjectRef,
        Set<EvidenceRef> evidenceRefs,
        ConclusionText conclusionText) {

    public RecordDecisionSpec {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(subjectRef, "subjectRef must not be null");
        Objects.requireNonNull(evidenceRefs, "evidenceRefs must not be null");
        Objects.requireNonNull(conclusionText, "conclusionText must not be null");
        if (evidenceRefs.isEmpty() || evidenceRefs.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("at least one evidenceRef is required");
        }
        evidenceRefs = Collections.unmodifiableSet(new LinkedHashSet<>(evidenceRefs));
    }
}
