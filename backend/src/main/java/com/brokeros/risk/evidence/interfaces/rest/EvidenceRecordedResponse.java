package com.brokeros.risk.evidence.interfaces.rest;

import java.time.Instant;

import com.brokeros.risk.evidence.application.EvidenceCorrectionResult;
import com.brokeros.risk.evidence.application.EvidenceRecordingResult;
import com.brokeros.risk.evidence.domain.EvidenceOperationOutcome;

public record EvidenceRecordedResponse(
        String evidenceRef,
        EvidenceOperationOutcome outcome,
        Instant occurredAt) {

    static EvidenceRecordedResponse from(EvidenceRecordingResult result) {
        return new EvidenceRecordedResponse(
                result.evidenceRef().value(), result.outcome(), result.occurredAt());
    }

    static EvidenceRecordedResponse from(EvidenceCorrectionResult result) {
        return new EvidenceRecordedResponse(
                result.evidenceRef().value(), result.outcome(), result.occurredAt());
    }
}
