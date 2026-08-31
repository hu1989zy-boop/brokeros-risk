package com.brokeros.risk.decision.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record RecordDecisionCommand(
        String operationId,
        String subjectRef,
        List<String> evidenceRefs,
        String conclusionText) {

    public RecordDecisionCommand {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(subjectRef, "subjectRef must not be null");
        Objects.requireNonNull(evidenceRefs, "evidenceRefs must not be null");
        Objects.requireNonNull(conclusionText, "conclusionText must not be null");
        evidenceRefs = Collections.unmodifiableList(new ArrayList<>(evidenceRefs));
    }
}
