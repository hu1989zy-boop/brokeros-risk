package com.brokeros.risk.evidence.infrastructure.configuration;

import java.time.Clock;
import java.util.UUID;

import com.brokeros.risk.evidence.application.EvidenceCorrectionService;
import com.brokeros.risk.evidence.application.EvidenceDetailReadService;
import com.brokeros.risk.evidence.application.EvidenceFingerprintFactory;
import com.brokeros.risk.evidence.application.EvidenceProvenanceQueryService;
import com.brokeros.risk.evidence.application.EvidenceRecordingService;
import com.brokeros.risk.evidence.application.EvidenceReferenceListService;
import com.brokeros.risk.evidence.application.port.EvidenceAccessLogPort;
import com.brokeros.risk.evidence.application.port.EvidenceMutationPort;
import com.brokeros.risk.evidence.application.port.EvidenceMetricsPort;
import com.brokeros.risk.evidence.application.port.EvidenceQueryPort;
import com.brokeros.risk.evidence.application.port.EvidenceRefGenerator;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.tradingaccount.application.TradingAccountReferenceEligibilityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class EvidenceModuleConfiguration {

    @Bean
    EvidenceRefGenerator evidenceRefGenerator() {
        return () -> new EvidenceRef("ev-" + UUID.randomUUID());
    }

    @Bean
    EvidenceFingerprintFactory evidenceFingerprintFactory() {
        return new EvidenceFingerprintFactory();
    }

    @Bean
    EvidenceRecordingService evidenceRecordingService(
            AuthorizationGuard authorizationGuard,
            EvidenceQueryPort queryPort,
            EvidenceMutationPort mutationPort,
            EvidenceFingerprintFactory fingerprintFactory,
            TradingAccountReferenceEligibilityService eligibilityService,
            EvidenceMetricsPort metrics,
            Clock securityClock) {
        return new EvidenceRecordingService(
                authorizationGuard, queryPort, mutationPort, fingerprintFactory,
                eligibilityService, metrics, securityClock);
    }

    @Bean
    EvidenceCorrectionService evidenceCorrectionService(
            AuthorizationGuard authorizationGuard,
            EvidenceQueryPort queryPort,
            EvidenceMutationPort mutationPort,
            EvidenceFingerprintFactory fingerprintFactory,
            EvidenceMetricsPort metrics,
            Clock securityClock) {
        return new EvidenceCorrectionService(
                authorizationGuard, queryPort, mutationPort,
                fingerprintFactory, metrics, securityClock);
    }

    @Bean
    EvidenceProvenanceQueryService evidenceProvenanceQueryService(
            AuthorizationGuard authorizationGuard,
            EvidenceQueryPort queryPort,
            EvidenceMetricsPort metrics) {
        return new EvidenceProvenanceQueryService(
                authorizationGuard, queryPort, metrics);
    }

    @Bean
    EvidenceDetailReadService evidenceDetailReadService(
            AuthorizationGuard authorizationGuard,
            EvidenceQueryPort queryPort,
            EvidenceAccessLogPort accessLogPort,
            EvidenceMetricsPort metrics,
            Clock securityClock) {
        return new EvidenceDetailReadService(
                authorizationGuard, queryPort, accessLogPort, metrics, securityClock);
    }

    @Bean
    EvidenceReferenceListService evidenceReferenceListService(
            AuthorizationGuard authorizationGuard,
            EvidenceQueryPort queryPort,
            EvidenceMetricsPort metrics) {
        return new EvidenceReferenceListService(authorizationGuard, queryPort, metrics);
    }
}
