package com.brokeros.risk.riskcase.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRiskCaseRequest(
        @NotBlank @Pattern(regexp = "MANUAL|DECISION_DRIVEN") String intakeSource,
        @NotBlank @Pattern(regexp = "TRADING_ACCOUNT") String subjectType,
        @NotBlank @Size(max = 128) String subjectRef,
        @NotBlank @Size(max = 1000) String intakeSummary,
        @NotBlank @Pattern(regexp = "LOW|NORMAL|HIGH|CRITICAL") String priority,
        @Size(max = 128) String decisionRef) {
}
