package com.brokeros.risk.riskcase.interfaces.rest;

import java.time.Instant;

import com.brokeros.risk.riskcase.domain.InvestigationNote;

public record RiskCaseNoteResponse(
        String noteRef,
        String supersedesNoteRef,
        long version,
        String createdByRef,
        Instant createdAt) {

    public static RiskCaseNoteResponse from(
            InvestigationNote note, String supersedesNoteRef) {
        return new RiskCaseNoteResponse(
                note.noteRef().value(), supersedesNoteRef, note.caseVersion(),
                note.createdBy().value(), note.createdAt());
    }
}
