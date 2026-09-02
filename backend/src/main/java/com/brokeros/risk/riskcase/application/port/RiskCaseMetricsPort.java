package com.brokeros.risk.riskcase.application.port;

import java.time.Duration;

import com.brokeros.risk.riskcase.application.RiskCaseMetricOperation;
import com.brokeros.risk.security.domain.Capability;

public interface RiskCaseMetricsPort {

    void recordSuccess(RiskCaseMetricOperation operation);

    void recordConflict(String category);

    void recordAuthorizationDenied(Capability capability);

    void recordDuration(RiskCaseMetricOperation operation, Duration duration);
}
