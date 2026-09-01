package com.brokeros.risk.action.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecordActionRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
        String operationId,

        @NotBlank
        @Pattern(regexp = "^dec-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
        String decisionRef,

        @NotBlank
        @Size(max = 4000)
        String intentText) {
}
