package com.brokeros.risk.decision.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public record DecisionRecord(
        DecisionRef decisionRef,
        TradingAccountRef subjectRef,
        Set<EvidenceRef> evidenceRefs,
        ConclusionText conclusionText,
        DecisionSource source,
        ActorRef recordedByActorRef,
        Instant recordedAt) {

    public DecisionRecord {
        Objects.requireNonNull(decisionRef, "decisionRef must not be null");
        Objects.requireNonNull(subjectRef, "subjectRef must not be null");
        Objects.requireNonNull(evidenceRefs, "evidenceRefs must not be null");
        Objects.requireNonNull(conclusionText, "conclusionText must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(recordedByActorRef, "recordedByActorRef must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        if (evidenceRefs.isEmpty() || evidenceRefs.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("decision requires at least one evidence reference");
        }
        TreeSet<EvidenceRef> sorted = new TreeSet<>(
                java.util.Comparator.comparing(EvidenceRef::value));
        sorted.addAll(evidenceRefs);
        evidenceRefs = Collections.unmodifiableSet(new LinkedHashSet<>(sorted));
    }
}
