package com.brokeros.risk.riskcase.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AssociateRiskCaseActionRequest(
        @NotBlank @Size(max = 128) String actionRef,
        @NotBlank @Size(max = 1000) String reason,
        @PositiveOrZero long expectedVersion) {
}
