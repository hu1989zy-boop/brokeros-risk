package com.brokeros.risk.actionoutcome.application.port;

import java.time.Duration;

import com.brokeros.risk.actionoutcome.application.ActionOutcomeMetricOperation;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationOutcome;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.security.domain.Capability;

public interface ActionOutcomeMetricsPort {

    void recordOperation(
            ActionOutcomeMetricOperation operation,
            ActionOutcomeOperationOutcome outcome);

    void recordConflict(ResultCode category);

    void recordAuthorizationDenied(Capability capability);

    void recordAccessRead(String outcome);

    void recordDuration(ActionOutcomeMetricOperation operation, Duration duration);
}
