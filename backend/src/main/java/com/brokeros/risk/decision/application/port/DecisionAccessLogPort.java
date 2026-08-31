package com.brokeros.risk.decision.application.port;

import java.time.Instant;

import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.domain.ActorRef;

public interface DecisionAccessLogPort {

    void recordFullDetailAccess(
            DecisionRef ref,
            ActorRef accessor,
            Instant occurredAt);
}
