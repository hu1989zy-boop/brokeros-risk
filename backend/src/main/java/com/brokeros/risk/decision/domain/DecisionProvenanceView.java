package com.brokeros.risk.decision.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public record DecisionProvenanceView(
        DecisionRef decisionRef,
        DecisionProvenanceOutcome outcome,
        TradingAccountRef subjectRef,
        Set<EvidenceRef> evidenceRefs,
        ActorRef recordedByActorRef,
        Instant recordedAt) {

    public DecisionProvenanceView {
        Objects.requireNonNull(decisionRef, "decisionRef must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        boolean complete = subjectRef != null && evidenceRefs != null && !evidenceRefs.isEmpty()
                && evidenceRefs.stream().noneMatch(Objects::isNull)
                && recordedByActorRef != null && recordedAt != null;
        if (outcome == DecisionProvenanceOutcome.RECOGNIZED && !complete) {
            throw new IllegalArgumentException("recognized provenance must be complete");
        }
        if (outcome == DecisionProvenanceOutcome.NOT_FOUND
                && (subjectRef != null || evidenceRefs != null
                || recordedByActorRef != null || recordedAt != null)) {
            throw new IllegalArgumentException("not-found provenance cannot disclose metadata");
        }
        if (evidenceRefs != null) {
            evidenceRefs = Collections.unmodifiableSet(new LinkedHashSet<>(evidenceRefs));
        }
    }

    public static DecisionProvenanceView recognized(DecisionRecord record) {
        return new DecisionProvenanceView(
                record.decisionRef(), DecisionProvenanceOutcome.RECOGNIZED,
                record.subjectRef(), record.evidenceRefs(),
                record.recordedByActorRef(), record.recordedAt());
    }

    public static DecisionProvenanceView notFound(DecisionRef ref) {
        return new DecisionProvenanceView(
                ref, DecisionProvenanceOutcome.NOT_FOUND,
                null, null, null, null);
    }
}
