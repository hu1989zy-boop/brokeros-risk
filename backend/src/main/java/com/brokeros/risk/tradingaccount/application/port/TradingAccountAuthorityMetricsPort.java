package com.brokeros.risk.tradingaccount.application.port;

import java.time.Duration;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationOutcome;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationType;

public interface TradingAccountAuthorityMetricsPort {
    void recordOperation(AuthorityOperationType type, AuthorityOperationOutcome outcome);
    void recordDuration(AuthorityOperationType type, Duration duration);
}
