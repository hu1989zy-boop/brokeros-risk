package com.brokeros.risk.action.interfaces.rest;

import java.time.Instant;

import com.brokeros.risk.action.domain.ActionRecord;
import com.brokeros.risk.action.domain.ActionSource;
import com.brokeros.risk.action.domain.ActionStatus;

public record ActionDetailResponse(
        String actionRef,
        String decisionRef,
        ActionSource source,
        ActionStatus status,
        String intentText,
        String recordedByActorRef,
        Instant recordedAt) {

    static ActionDetailResponse from(ActionRecord record) {
        return new ActionDetailResponse(
                record.actionRef().value(),
                record.decisionRef().value(),
                record.source(),
                record.status(),
                record.intentText().value(),
                record.recordedByActorRef().value(),
                record.recordedAt());
    }
}
