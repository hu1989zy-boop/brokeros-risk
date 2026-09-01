package com.brokeros.risk.actionoutcome.application.port;

import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;

@FunctionalInterface
public interface ActionOutcomeRefGenerator {

    ActionOutcomeRef generate();
}
