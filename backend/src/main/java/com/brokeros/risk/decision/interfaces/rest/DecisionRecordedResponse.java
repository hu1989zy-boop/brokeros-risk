package com.brokeros.risk.decision.interfaces.rest;

import java.time.Instant;
import java.util.List;

import com.brokeros.risk.decision.application.CompletedDecisionOperation;
import com.brokeros.risk.decision.domain.DecisionOperationOutcome;

public record DecisionRecordedResponse(
        String decisionRef,
        String subjectRef,
        List<String> evidenceRefs,
        String recordedByActorRef,
        Instant recordedAt,
        DecisionOperationOutcome outcome) {

    static DecisionRecordedResponse from(CompletedDecisionOperation operation) {
        return new DecisionRecordedResponse(
                operation.decisionRef().value(),
                operation.decisionRecord().subjectRef().value(),
                operation.decisionRecord().evidenceRefs().stream()
                        .map(ref -> ref.value()).toList(),
                operation.decisionRecord().recordedByActorRef().value(),
                operation.decisionRecord().recordedAt(),
                operation.outcome());
    }
}
