package com.brokeros.risk.riskcase.interfaces.rest;

import java.time.Instant;

import com.brokeros.risk.riskcase.domain.Assignment;
import com.brokeros.risk.riskcase.domain.RiskCaseSnapshot;

public record RiskCaseDetailResponse(
        String caseNumber,
        String subjectType,
        String subjectRef,
        String intakeSource,
        String intakeSummary,
        String status,
        String priority,
        String assigneeRef,
        String assignedByRef,
        Instant assignedAt,
        String currentDecisionRef,
        int currentCycleNo,
        String createdByRef,
        Instant createdAt,
        String updatedByRef,
        Instant updatedAt,
        long version) {

    public static RiskCaseDetailResponse from(RiskCaseSnapshot snapshot) {
        Assignment assignment = snapshot.assignment();
        return new RiskCaseDetailResponse(
                snapshot.caseNumber().value(), snapshot.subjectRef().subjectType().name(),
                snapshot.subjectRef().value(), snapshot.intakeSource().name(),
                snapshot.intakeSummary(), snapshot.status().name(), snapshot.priority().name(),
                assignment == null ? null : assignment.assignee().value(),
                assignment == null ? null : assignment.assignedBy().value(),
                assignment == null ? null : assignment.assignedAt(),
                snapshot.currentDecisionRef() == null
                        ? null
                        : snapshot.currentDecisionRef().value(),
                snapshot.currentCycle().value(), snapshot.createdBy().value(),
                snapshot.createdAt(), snapshot.updatedBy().value(), snapshot.updatedAt(),
                snapshot.version());
    }
}
