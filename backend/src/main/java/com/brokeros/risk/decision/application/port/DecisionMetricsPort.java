package com.brokeros.risk.decision.application.port;

import java.time.Duration;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.application.DecisionMetricOperation;
import com.brokeros.risk.decision.domain.DecisionOperationOutcome;
import com.brokeros.risk.security.domain.Capability;

public interface DecisionMetricsPort {

    void recordOperation(
            DecisionMetricOperation operation,
            DecisionOperationOutcome outcome);

    void recordConflict(ResultCode category);

    void recordAuthorizationDenied(Capability capability);

    void recordAccessRead(String outcome);

    void recordDuration(DecisionMetricOperation operation, Duration duration);
}
