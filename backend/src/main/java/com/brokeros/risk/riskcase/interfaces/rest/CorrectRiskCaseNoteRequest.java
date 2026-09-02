package com.brokeros.risk.riskcase.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CorrectRiskCaseNoteRequest(
        @NotBlank @Size(max = 4000) String content,
        @PositiveOrZero long expectedVersion) {
}
