package com.brokeros.risk.riskcase.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AssociateRiskCaseEvidenceRequest(
        @NotBlank @Size(max = 128) String evidenceRef,
        @NotBlank @Size(max = 1000) String reason,
        @NotBlank @Size(max = 64) String source,
        @PositiveOrZero long expectedVersion) {
}
