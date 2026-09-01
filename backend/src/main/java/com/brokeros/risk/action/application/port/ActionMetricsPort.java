package com.brokeros.risk.action.application.port;

import java.time.Duration;

import com.brokeros.risk.action.application.ActionMetricOperation;
import com.brokeros.risk.action.domain.ActionOperationOutcome;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.security.domain.Capability;

public interface ActionMetricsPort {

    void recordOperation(
            ActionMetricOperation operation,
            ActionOperationOutcome outcome);

    void recordConflict(ResultCode category);

    void recordAuthorizationDenied(Capability capability);

    void recordAccessRead(String outcome);

    void recordDuration(ActionMetricOperation operation, Duration duration);
}
