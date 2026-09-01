package com.brokeros.risk.action.infrastructure.configuration;

import java.time.Clock;
import java.util.UUID;

import com.brokeros.risk.action.application.ActionDetailReadService;
import com.brokeros.risk.action.application.ActionFingerprintFactory;
import com.brokeros.risk.action.application.ActionProvenanceQueryService;
import com.brokeros.risk.action.application.ActionRecordingService;
import com.brokeros.risk.action.application.AuthorizedMutationFactory;
import com.brokeros.risk.action.application.port.ActionAccessLogPort;
import com.brokeros.risk.action.application.port.ActionMetricsPort;
import com.brokeros.risk.action.application.port.ActionMutationPort;
import com.brokeros.risk.action.application.port.ActionQueryPort;
import com.brokeros.risk.action.application.port.ActionRefGenerator;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.decision.application.DecisionProvenanceQueryService;
import com.brokeros.risk.security.application.AuthorizationGuard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ActionModuleConfiguration {

    @Bean
    ActionRefGenerator actionRefGenerator() {
        return () -> new ActionRef("act-" + UUID.randomUUID());
    }

    @Bean
    ActionFingerprintFactory actionFingerprintFactory() {
        return new ActionFingerprintFactory();
    }

    @Bean
    AuthorizedMutationFactory actionAuthorizedMutationFactory(Clock securityClock) {
        return new AuthorizedMutationFactory(securityClock);
    }

    @Bean
    ActionRecordingService actionRecordingService(
            AuthorizationGuard authorizationGuard,
            ActionQueryPort queryPort,
            ActionMutationPort mutationPort,
            ActionFingerprintFactory fingerprintFactory,
            DecisionProvenanceQueryService decisionQueryService,
            AuthorizedMutationFactory actionAuthorizedMutationFactory,
            ActionMetricsPort metrics) {
        return new ActionRecordingService(
                authorizationGuard, queryPort, mutationPort, fingerprintFactory,
                decisionQueryService, actionAuthorizedMutationFactory, metrics);
    }

    @Bean
    ActionProvenanceQueryService actionProvenanceQueryService(
            AuthorizationGuard authorizationGuard,
            ActionQueryPort queryPort,
            ActionMetricsPort metrics) {
        return new ActionProvenanceQueryService(
                authorizationGuard, queryPort, metrics);
    }

    @Bean
    ActionDetailReadService actionDetailReadService(
            AuthorizationGuard authorizationGuard,
            ActionQueryPort queryPort,
            ActionAccessLogPort accessLogPort,
            ActionMetricsPort metrics,
            Clock securityClock) {
        return new ActionDetailReadService(
                authorizationGuard, queryPort, accessLogPort, metrics, securityClock);
    }
}
