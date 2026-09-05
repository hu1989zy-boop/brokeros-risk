package com.brokeros.risk.actionoutcome.infrastructure.configuration;

import java.time.Clock;
import java.util.UUID;

import com.brokeros.risk.action.application.ActionProvenanceQueryService;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeDetailReadService;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeFingerprintFactory;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeProvenanceQueryService;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeRecordingService;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeReferenceListService;
import com.brokeros.risk.actionoutcome.application.AuthorizedMutationFactory;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeAccessLogPort;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeMetricsPort;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeMutationPort;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeQueryPort;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeRefGenerator;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.security.application.AuthorizationGuard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ActionOutcomeModuleConfiguration {

    @Bean
    ActionOutcomeRefGenerator actionOutcomeRefGenerator() {
        return () -> new ActionOutcomeRef("aoc-" + UUID.randomUUID());
    }

    @Bean
    ActionOutcomeFingerprintFactory actionOutcomeFingerprintFactory() {
        return new ActionOutcomeFingerprintFactory();
    }

    @Bean
    AuthorizedMutationFactory actionOutcomeAuthorizedMutationFactory(
            Clock securityClock) {
        return new AuthorizedMutationFactory(securityClock);
    }

    @Bean
    ActionOutcomeRecordingService actionOutcomeRecordingService(
            AuthorizationGuard authorizationGuard,
            ActionOutcomeQueryPort queryPort,
            ActionOutcomeMutationPort mutationPort,
            ActionOutcomeFingerprintFactory fingerprintFactory,
            ActionProvenanceQueryService actionQueryService,
            AuthorizedMutationFactory actionOutcomeAuthorizedMutationFactory,
            ActionOutcomeMetricsPort metrics) {
        return new ActionOutcomeRecordingService(
                authorizationGuard, queryPort, mutationPort, fingerprintFactory,
                actionQueryService, actionOutcomeAuthorizedMutationFactory, metrics);
    }

    @Bean
    ActionOutcomeProvenanceQueryService actionOutcomeProvenanceQueryService(
            AuthorizationGuard authorizationGuard,
            ActionOutcomeQueryPort queryPort,
            ActionOutcomeMetricsPort metrics) {
        return new ActionOutcomeProvenanceQueryService(
                authorizationGuard, queryPort, metrics);
    }

    @Bean
    ActionOutcomeDetailReadService actionOutcomeDetailReadService(
            AuthorizationGuard authorizationGuard,
            ActionOutcomeQueryPort queryPort,
            ActionOutcomeAccessLogPort accessLogPort,
            ActionOutcomeMetricsPort metrics,
            Clock securityClock) {
        return new ActionOutcomeDetailReadService(
                authorizationGuard, queryPort, accessLogPort, metrics, securityClock);
    }

    @Bean
    ActionOutcomeReferenceListService actionOutcomeReferenceListService(
            AuthorizationGuard authorizationGuard,
            ActionOutcomeQueryPort queryPort,
            ActionOutcomeMetricsPort metrics) {
        return new ActionOutcomeReferenceListService(
                authorizationGuard, queryPort, metrics);
    }
}
