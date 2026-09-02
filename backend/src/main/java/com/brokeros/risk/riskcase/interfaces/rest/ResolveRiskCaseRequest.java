package com.brokeros.risk.riskcase.interfaces.rest;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ResolveRiskCaseRequest(
        @NotBlank @Pattern(regexp = "RISK_CONFIRMED_ACTION_COMPLETED|NO_RISK|FALSE_POSITIVE|MONITORING_ONLY|NO_ACTION_REQUIRED") String outcome,
        @NotBlank @Size(max = 2000) String resolutionSummary,
        @NotNull @Size(max = 100) Set<@NotBlank @Size(max = 128) String> evidenceRefs,
        @NotNull @Size(max = 100) Set<@NotBlank @Size(max = 128) String> actionRefs,
        @PositiveOrZero long expectedVersion) {
}
