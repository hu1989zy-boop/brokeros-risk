package com.brokeros.risk.decision.interfaces.rest;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecordDecisionRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
        String operationId,

        @NotBlank
        @Pattern(regexp = "^ta-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
        String subjectRef,

        @NotEmpty
        List<@NotBlank @Pattern(
                regexp = "^ev-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$") String> evidenceRefs,

        @NotBlank
        @Size(max = 4000)
        String conclusionText) {
}
