package com.brokeros.risk.riskcase.application;

import java.time.Instant;
import java.util.UUID;

import com.brokeros.risk.audit.domain.AuditRecord;
import com.brokeros.risk.riskcase.domain.RiskCaseSnapshot;
import com.brokeros.risk.security.domain.ActorContext;

public final class RiskCaseAuditFactory {

    public AuditRecord material(
            RiskCaseSnapshot before,
            RiskCaseSnapshot after,
            ActorContext actorContext,
            Instant occurredAt,
            String operationCode,
            String affectedRefType,
            String affectedRef,
            String reason) {
        return record(after, actorContext, occurredAt, operationCode,
                affectedRefType, affectedRef, reason,
                before == null ? null : safeState(before), safeState(after), after.version());
    }

    public AuditRecord read(
            RiskCaseSnapshot snapshot,
            ActorContext actorContext,
            Instant occurredAt,
            String operationCode) {
        return record(snapshot, actorContext, occurredAt, operationCode,
                null, null, "authorized sensitive-content access",
                null, safeState(snapshot), null);
    }

    private AuditRecord record(
            RiskCaseSnapshot snapshot,
            ActorContext actorContext,
            Instant occurredAt,
            String operationCode,
            String affectedRefType,
            String affectedRef,
            String reason,
            String beforeState,
            String afterState,
            Long caseVersion) {
        return new AuditRecord(
                UUID.randomUUID().toString(), "RISK_CASE", snapshot.id().value(),
                snapshot.caseNumber().value(), caseVersion, operationCode,
                affectedRefType, affectedRef, actorContext.actorRef(), occurredAt,
                reason, "RISK_CASE", actorContext.requestId(), actorContext.traceId(),
                beforeState, afterState);
    }

    private String safeState(RiskCaseSnapshot snapshot) {
        String assignee = snapshot.assignment() == null
                ? "null"
                : quote(snapshot.assignment().assignee().value());
        String decision = snapshot.currentDecisionRef() == null
                ? "null"
                : quote(snapshot.currentDecisionRef().value());
        return "{\"status\":" + quote(snapshot.status().name())
                + ",\"priority\":" + quote(snapshot.priority().name())
                + ",\"assigneeRef\":" + assignee
                + ",\"decisionRef\":" + decision
                + ",\"cycleNo\":" + snapshot.currentCycle().value()
                + ",\"version\":" + snapshot.version() + "}";
    }

    private String quote(String value) {
        return "\"" + value + "\"";
    }
}
