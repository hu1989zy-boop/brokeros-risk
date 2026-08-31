package com.brokeros.risk.evidence.application.port;

import java.time.Instant;

import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.security.domain.ActorRef;

public interface EvidenceAccessLogPort {

    void recordFullDetailAccess(
            EvidenceRef ref,
            ActorRef accessor,
            Instant occurredAt);
}
