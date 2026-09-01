package com.brokeros.risk.actionoutcome.interfaces.rest;

import java.time.Instant;

import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRecord;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeSource;

public record ActionOutcomeDetailResponse(
        String actionOutcomeRef,
        String actionRef,
        ActionOutcomeSource source,
        String outcomeText,
        String recordedByActorRef,
        Instant recordedAt) {

    static ActionOutcomeDetailResponse from(ActionOutcomeRecord record) {
        return new ActionOutcomeDetailResponse(
                record.actionOutcomeRef().value(),
                record.actionRef().value(),
                record.source(),
                record.outcomeText().value(),
                record.recordedByActorRef().value(),
                record.recordedAt());
    }
}
