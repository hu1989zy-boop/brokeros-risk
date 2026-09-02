package com.brokeros.risk.riskcase.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ChangeRiskCasePriorityRequest(
        @NotBlank @Pattern(regexp = "LOW|NORMAL|HIGH|CRITICAL") String priority,
        @NotBlank @Size(max = 1000) String reason,
        @PositiveOrZero long expectedVersion) {
}
