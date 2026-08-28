package com.brokeros.risk.tradingaccount.infrastructure.configuration;

import java.time.Clock;
import java.util.UUID;

import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.tradingaccount.application.AuthorityEvidenceFactory;
import com.brokeros.risk.tradingaccount.application.AuthorityScopeLifecycleService;
import com.brokeros.risk.tradingaccount.application.AuthorityScopeProvisioningService;
import com.brokeros.risk.tradingaccount.application.AuthorizedMutationFactory;
import com.brokeros.risk.tradingaccount.application.ExternalIdentityResolutionService;
import com.brokeros.risk.tradingaccount.application.ManifestFingerprintFactory;
import com.brokeros.risk.tradingaccount.application.TradingAccountLifecycleService;
import com.brokeros.risk.tradingaccount.application.TradingAccountReferenceEligibilityService;
import com.brokeros.risk.tradingaccount.application.TradingAccountRegistrationService;
import com.brokeros.risk.tradingaccount.application.port.AccountAuthorityScopeRefGenerator;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountAuthorityMutationPort;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountAuthorityQueryPort;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountRefGenerator;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountAuthorityMetricsPort;
import com.brokeros.risk.tradingaccount.domain.AccountAuthorityScopeRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TradingAccountAuthorityConfiguration {

    @Bean
    TradingAccountReferenceProvisionerDescriptor tradingAccountReferenceProvisionerDescriptor() {
        return new TradingAccountReferenceProvisionerDescriptor();
    }

    @Bean
    AccountAuthorityScopeRefGenerator accountAuthorityScopeRefGenerator() {
        return () -> new AccountAuthorityScopeRef("aas-" + UUID.randomUUID());
    }

    @Bean
    TradingAccountRefGenerator tradingAccountRefGenerator() {
        return () -> new TradingAccountRef("ta-" + UUID.randomUUID());
    }

    @Bean
    ManifestFingerprintFactory manifestFingerprintFactory() {
        return new ManifestFingerprintFactory();
    }

    @Bean
    AuthorizedMutationFactory authorizedMutationFactory(
            AuthorizationGuard authorizationGuard,
            ManifestFingerprintFactory fingerprintFactory,
            Clock securityClock) {
        return new AuthorizedMutationFactory(authorizationGuard, fingerprintFactory, securityClock);
    }

    @Bean
    AuthorityScopeProvisioningService authorityScopeProvisioningService(
            AuthorizedMutationFactory factory,
            TradingAccountAuthorityMutationPort mutationPort,
            TradingAccountAuthorityMetricsPort metrics) {
        return new AuthorityScopeProvisioningService(factory, mutationPort, metrics);
    }

    @Bean
    TradingAccountRegistrationService tradingAccountRegistrationService(
            AuthorizedMutationFactory factory,
            TradingAccountAuthorityMutationPort mutationPort,
            TradingAccountAuthorityMetricsPort metrics) {
        return new TradingAccountRegistrationService(factory, mutationPort, metrics);
    }

    @Bean
    AuthorityScopeLifecycleService authorityScopeLifecycleService(
            AuthorizedMutationFactory factory,
            TradingAccountAuthorityMutationPort mutationPort,
            TradingAccountAuthorityMetricsPort metrics) {
        return new AuthorityScopeLifecycleService(factory, mutationPort, metrics);
    }

    @Bean
    TradingAccountLifecycleService tradingAccountLifecycleService(
            AuthorizedMutationFactory factory,
            TradingAccountAuthorityMutationPort mutationPort,
            TradingAccountAuthorityMetricsPort metrics) {
        return new TradingAccountLifecycleService(factory, mutationPort, metrics);
    }

    @Bean
    ExternalIdentityResolutionService externalIdentityResolutionService(
            AuthorizationGuard authorizationGuard,
            TradingAccountAuthorityQueryPort queryPort) {
        return new ExternalIdentityResolutionService(authorizationGuard, queryPort);
    }

    @Bean
    AuthorityEvidenceFactory authorityEvidenceFactory() {
        return new AuthorityEvidenceFactory();
    }

    @Bean
    TradingAccountReferenceEligibilityService tradingAccountReferenceEligibilityService(
            AuthorizationGuard authorizationGuard,
            TradingAccountAuthorityQueryPort queryPort,
            AuthorityEvidenceFactory evidenceFactory) {
        return new TradingAccountReferenceEligibilityService(
                authorizationGuard, queryPort, evidenceFactory);
    }
}
