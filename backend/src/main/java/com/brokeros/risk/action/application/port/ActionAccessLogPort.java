package com.brokeros.risk.action.application.port;

import java.time.Instant;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.security.domain.ActorRef;

public interface ActionAccessLogPort {

    void recordFullDetailAccess(
            ActionRef ref,
            ActorRef accessor,
            Instant occurredAt);
}
