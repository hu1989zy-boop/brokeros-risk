package com.brokeros.risk.decision.infrastructure.configuration;

import java.time.Clock;
import java.util.UUID;

import com.brokeros.risk.decision.application.AuthorizedMutationFactory;
import com.brokeros.risk.decision.application.DecisionDetailReadService;
import com.brokeros.risk.decision.application.DecisionFingerprintFactory;
import com.brokeros.risk.decision.application.DecisionProvenanceQueryService;
import com.brokeros.risk.decision.application.DecisionRecordingService;
import com.brokeros.risk.decision.application.DecisionReferenceListService;
import com.brokeros.risk.decision.application.port.DecisionAccessLogPort;
import com.brokeros.risk.decision.application.port.DecisionMetricsPort;
import com.brokeros.risk.decision.application.port.DecisionMutationPort;
import com.brokeros.risk.decision.application.port.DecisionQueryPort;
import com.brokeros.risk.decision.application.port.DecisionRefGenerator;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.evidence.application.EvidenceProvenanceQueryService;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.tradingaccount.application.TradingAccountReferenceEligibilityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DecisionModuleConfiguration {

    @Bean
    DecisionRefGenerator decisionRefGenerator() {
        return () -> new DecisionRef("dec-" + UUID.randomUUID());
    }

    @Bean
    DecisionFingerprintFactory decisionFingerprintFactory() {
        return new DecisionFingerprintFactory();
    }

    @Bean
    AuthorizedMutationFactory decisionAuthorizedMutationFactory(Clock securityClock) {
        return new AuthorizedMutationFactory(securityClock);
    }

    @Bean
    DecisionRecordingService decisionRecordingService(
            AuthorizationGuard authorizationGuard,
            DecisionQueryPort queryPort,
            DecisionMutationPort mutationPort,
            DecisionFingerprintFactory fingerprintFactory,
            TradingAccountReferenceEligibilityService eligibilityService,
            EvidenceProvenanceQueryService evidenceQueryService,
            AuthorizedMutationFactory decisionAuthorizedMutationFactory,
            DecisionMetricsPort metrics) {
        return new DecisionRecordingService(
                authorizationGuard, queryPort, mutationPort, fingerprintFactory,
                eligibilityService, evidenceQueryService,
                decisionAuthorizedMutationFactory, metrics);
    }

    @Bean
    DecisionProvenanceQueryService decisionProvenanceQueryService(
            AuthorizationGuard authorizationGuard,
            DecisionQueryPort queryPort,
            DecisionMetricsPort metrics) {
        return new DecisionProvenanceQueryService(
                authorizationGuard, queryPort, metrics);
    }

    @Bean
    DecisionDetailReadService decisionDetailReadService(
            AuthorizationGuard authorizationGuard,
            DecisionQueryPort queryPort,
            DecisionAccessLogPort accessLogPort,
            DecisionMetricsPort metrics,
            Clock securityClock) {
        return new DecisionDetailReadService(
                authorizationGuard, queryPort, accessLogPort, metrics, securityClock);
    }

    @Bean
    DecisionReferenceListService decisionReferenceListService(
            AuthorizationGuard authorizationGuard,
            DecisionQueryPort queryPort,
            DecisionMetricsPort metrics) {
        return new DecisionReferenceListService(authorizationGuard, queryPort, metrics);
    }
}
