package com.brokeros.risk.actionoutcome.application.port;

import java.time.Instant;

import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.security.domain.ActorRef;

public interface ActionOutcomeAccessLogPort {

    void recordFullDetailAccess(
            ActionOutcomeRef ref,
            ActorRef accessor,
            Instant occurredAt);
}
