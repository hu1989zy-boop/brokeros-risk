package com.brokeros.risk.decision.interfaces.rest;

import java.time.Instant;
import java.util.List;

import com.brokeros.risk.decision.domain.DecisionRecord;
import com.brokeros.risk.decision.domain.DecisionSource;

public record DecisionDetailResponse(
        String decisionRef,
        String subjectRef,
        List<String> evidenceRefs,
        DecisionSource source,
        String conclusionText,
        String recordedByActorRef,
        Instant recordedAt) {

    static DecisionDetailResponse from(DecisionRecord record) {
        return new DecisionDetailResponse(
                record.decisionRef().value(),
                record.subjectRef().value(),
                record.evidenceRefs().stream().map(ref -> ref.value()).toList(),
                record.source(),
                record.conclusionText().value(),
                record.recordedByActorRef().value(),
                record.recordedAt());
    }
}
