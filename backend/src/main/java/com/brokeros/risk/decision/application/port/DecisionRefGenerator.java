package com.brokeros.risk.decision.application.port;

import com.brokeros.risk.decision.domain.DecisionRef;

@FunctionalInterface
public interface DecisionRefGenerator {

    DecisionRef generate();
}
