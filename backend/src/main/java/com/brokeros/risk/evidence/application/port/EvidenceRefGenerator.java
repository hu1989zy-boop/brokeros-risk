package com.brokeros.risk.evidence.application.port;

import com.brokeros.risk.evidence.domain.EvidenceRef;

@FunctionalInterface
public interface EvidenceRefGenerator {

    EvidenceRef generate();
}
