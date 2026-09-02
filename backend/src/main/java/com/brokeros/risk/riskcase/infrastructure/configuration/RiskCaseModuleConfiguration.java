package com.brokeros.risk.riskcase.infrastructure.configuration;

import java.time.Clock;
import java.util.UUID;

import com.brokeros.risk.audit.application.port.AuditRecordWriter;
import com.brokeros.risk.riskcase.application.RiskCaseAssociationService;
import com.brokeros.risk.riskcase.application.RiskCaseAuditFactory;
import com.brokeros.risk.riskcase.application.RiskCaseCommandService;
import com.brokeros.risk.riskcase.application.RiskCaseCreationService;
import com.brokeros.risk.riskcase.application.RiskCaseFingerprintFactory;
import com.brokeros.risk.riskcase.application.RiskCaseQueryService;
import com.brokeros.risk.riskcase.application.RiskCaseResolutionService;
import com.brokeros.risk.riskcase.application.port.ActionOutcomeReferenceQuery;
import com.brokeros.risk.riskcase.application.port.ActionReferenceQuery;
import com.brokeros.risk.riskcase.application.port.DecisionReferenceQuery;
import com.brokeros.risk.riskcase.application.port.EvidenceReferenceQuery;
import com.brokeros.risk.riskcase.application.port.RiskCaseMetricsPort;
import com.brokeros.risk.riskcase.application.port.RiskCaseRepository;
import com.brokeros.risk.riskcase.application.port.TradingAccountReferenceQuery;
import com.brokeros.risk.riskcase.domain.CaseNumber;
import com.brokeros.risk.riskcase.domain.CaseNumberGenerator;
import com.brokeros.risk.security.application.AuthorizationGuard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class RiskCaseModuleConfiguration {

    @Bean
    CaseNumberGenerator riskCaseNumberGenerator() {
        return () -> new CaseNumber("RC-" + UUID.randomUUID());
    }

    @Bean
    RiskCaseFingerprintFactory riskCaseFingerprintFactory() {
        return new RiskCaseFingerprintFactory();
    }

    @Bean
    RiskCaseAuditFactory riskCaseAuditFactory() {
        return new RiskCaseAuditFactory();
    }

    @Bean
    RiskCaseCreationService riskCaseCreationService(
            AuthorizationGuard authorizationGuard,
            TradingAccountReferenceQuery subjectQuery,
            DecisionReferenceQuery decisionQuery,
            RiskCaseRepository repository,
            AuditRecordWriter auditWriter,
            CaseNumberGenerator riskCaseNumberGenerator,
            RiskCaseFingerprintFactory riskCaseFingerprintFactory,
            RiskCaseAuditFactory riskCaseAuditFactory,
            RiskCaseMetricsPort metrics,
            Clock securityClock,
            PlatformTransactionManager transactionManager) {
        return new RiskCaseCreationService(
                authorizationGuard, subjectQuery, decisionQuery, repository,
                auditWriter, riskCaseNumberGenerator, riskCaseFingerprintFactory,
                riskCaseAuditFactory, metrics, securityClock, transactionManager);
    }

    @Bean
    RiskCaseCommandService riskCaseCommandService(
            AuthorizationGuard authorizationGuard,
            RiskCaseRepository repository,
            AuditRecordWriter auditWriter,
            RiskCaseAuditFactory riskCaseAuditFactory,
            RiskCaseMetricsPort metrics,
            Clock securityClock,
            PlatformTransactionManager transactionManager) {
        return new RiskCaseCommandService(
                authorizationGuard, repository, auditWriter, riskCaseAuditFactory,
                metrics, securityClock, transactionManager);
    }

    @Bean
    RiskCaseAssociationService riskCaseAssociationService(
            AuthorizationGuard authorizationGuard,
            EvidenceReferenceQuery evidenceQuery,
            DecisionReferenceQuery decisionQuery,
            ActionReferenceQuery actionQuery,
            ActionOutcomeReferenceQuery outcomeQuery,
            RiskCaseRepository repository,
            AuditRecordWriter auditWriter,
            RiskCaseAuditFactory riskCaseAuditFactory,
            RiskCaseMetricsPort metrics,
            Clock securityClock,
            PlatformTransactionManager transactionManager) {
        return new RiskCaseAssociationService(
                authorizationGuard, evidenceQuery, decisionQuery, actionQuery,
                outcomeQuery, repository, auditWriter, riskCaseAuditFactory,
                metrics, securityClock, transactionManager);
    }

    @Bean
    RiskCaseResolutionService riskCaseResolutionService(
            AuthorizationGuard authorizationGuard,
            DecisionReferenceQuery decisionQuery,
            EvidenceReferenceQuery evidenceQuery,
            ActionReferenceQuery actionQuery,
            RiskCaseRepository repository,
            AuditRecordWriter auditWriter,
            RiskCaseAuditFactory riskCaseAuditFactory,
            RiskCaseMetricsPort metrics,
            Clock securityClock,
            PlatformTransactionManager transactionManager) {
        return new RiskCaseResolutionService(
                authorizationGuard, decisionQuery, evidenceQuery, actionQuery,
                repository, auditWriter, riskCaseAuditFactory, metrics,
                securityClock, transactionManager);
    }

    @Bean
    RiskCaseQueryService riskCaseQueryService(
            AuthorizationGuard authorizationGuard,
            RiskCaseRepository repository,
            AuditRecordWriter auditWriter,
            RiskCaseAuditFactory riskCaseAuditFactory,
            RiskCaseMetricsPort metrics,
            Clock securityClock,
            PlatformTransactionManager transactionManager) {
        return new RiskCaseQueryService(
                authorizationGuard, repository, auditWriter, riskCaseAuditFactory,
                metrics, securityClock, transactionManager);
    }
}
