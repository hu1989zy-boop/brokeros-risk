package com.brokeros.risk.riskcase.application;

public record CreateRiskCaseCommand(
        String intakeSource,
        String subjectType,
        String subjectRef,
        String intakeSummary,
        String priority,
        String decisionRef,
        String idempotencyKey) {
}
