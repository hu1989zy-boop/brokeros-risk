package com.brokeros.risk.riskcase.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ChangeEvidenceAssociationDispositionRequest(
        @NotBlank @Pattern(regexp = "SUPERSEDED|INVALIDATED|WITHDRAWN") String disposition,
        @Size(max = 128) String replacementEvidenceRef,
        @NotBlank @Size(max = 1000) String reason,
        @NotBlank @Size(max = 64) String source,
        @PositiveOrZero long expectedVersion) {
}
