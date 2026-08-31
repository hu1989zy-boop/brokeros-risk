package com.brokeros.risk.evidence.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecordEvidenceRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
        String operationId,

        @NotBlank
        @Pattern(regexp = "^ta-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
        String subjectRef,

        @NotBlank
        @Size(max = 4000)
        String observationText) {
}
