package com.brokeros.risk.riskcase.domain;

@FunctionalInterface
public interface CaseNumberGenerator {

    CaseNumber generate();
}
