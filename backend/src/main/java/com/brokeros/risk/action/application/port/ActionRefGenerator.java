package com.brokeros.risk.action.application.port;

import com.brokeros.risk.action.domain.ActionRef;

@FunctionalInterface
public interface ActionRefGenerator {

    ActionRef generate();
}
