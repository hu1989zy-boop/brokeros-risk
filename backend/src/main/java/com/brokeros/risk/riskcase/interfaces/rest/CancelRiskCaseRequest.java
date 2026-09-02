package com.brokeros.risk.riskcase.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CancelRiskCaseRequest(
        @NotBlank @Size(max = 1000) String reason,
        @Size(max = 39) String duplicateCaseNumber,
        @PositiveOrZero long expectedVersion) {
}
