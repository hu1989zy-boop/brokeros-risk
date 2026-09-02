package com.brokeros.risk.riskcase.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.security.domain.ActorRef;

public record InvestigationNote(
        Long id,
        InvestigationNoteRef noteRef,
        RiskCaseId caseId,
        long caseVersion,
        String content,
        Long supersedesNoteId,
        ActorRef createdBy,
        Instant createdAt) {

    public InvestigationNote {
        Objects.requireNonNull(noteRef, "noteRef must not be null");
        Objects.requireNonNull(caseId, "caseId must not be null");
        content = RiskCaseText.require(content, 4000, "note content");
        Objects.requireNonNull(createdBy, "createdBy must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
