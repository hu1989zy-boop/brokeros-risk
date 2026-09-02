package com.brokeros.risk.audit.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.security.domain.ActorRef;

public record AuditRecord(
        String auditId,
        String targetType,
        long targetId,
        String targetBusinessRef,
        Long caseVersion,
        String operationCode,
        String affectedRefType,
        String affectedRef,
        ActorRef actorRef,
        Instant occurredAt,
        String reason,
        String source,
        String requestId,
        String traceId,
        String beforeState,
        String afterState) {

    public AuditRecord {
        Objects.requireNonNull(auditId, "auditId must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        if (targetId <= 0) {
            throw new IllegalArgumentException("targetId must be positive");
        }
        Objects.requireNonNull(targetBusinessRef, "targetBusinessRef must not be null");
        Objects.requireNonNull(operationCode, "operationCode must not be null");
        Objects.requireNonNull(actorRef, "actorRef must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(source, "source must not be null");
    }
}
