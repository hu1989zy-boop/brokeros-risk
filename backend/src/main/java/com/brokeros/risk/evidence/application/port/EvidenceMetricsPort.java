package com.brokeros.risk.evidence.application.port;

import java.time.Duration;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.evidence.application.EvidenceMetricOperation;
import com.brokeros.risk.evidence.domain.EvidenceOperationOutcome;
import com.brokeros.risk.security.domain.Capability;

public interface EvidenceMetricsPort {

    void recordOperation(
            EvidenceMetricOperation operation,
            EvidenceOperationOutcome outcome);

    void recordConflict(ResultCode category);

    void recordAuthorizationDenied(Capability capability);

    void recordAccessRead(String outcome);

    void recordDuration(EvidenceMetricOperation operation, Duration duration);
}
