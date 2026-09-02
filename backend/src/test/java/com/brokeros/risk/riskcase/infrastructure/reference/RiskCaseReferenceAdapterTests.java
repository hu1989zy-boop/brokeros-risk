package com.brokeros.risk.riskcase.infrastructure.reference;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import com.brokeros.risk.action.application.ActionAuthorityUnavailableException;
import com.brokeros.risk.action.application.ActionProvenanceQueryService;
import com.brokeros.risk.action.domain.ActionProvenanceView;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeAuthorityUnavailableException;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeProvenanceQueryService;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeProvenanceView;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.application.DecisionAuthorityUnavailableException;
import com.brokeros.risk.decision.application.DecisionProvenanceQueryService;
import com.brokeros.risk.decision.domain.DecisionProvenanceView;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.evidence.application.EvidenceAuthorityUnavailableException;
import com.brokeros.risk.evidence.application.EvidenceProvenanceQueryService;
import com.brokeros.risk.evidence.domain.EvidenceProvenanceView;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.riskcase.application.RiskCaseException;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.tradingaccount.application.TradingAccountAuthorityUnavailableException;
import com.brokeros.risk.tradingaccount.application.TradingAccountReferenceEligibilityService;
import com.brokeros.risk.tradingaccount.domain.AuthorityProvenanceRef;
import com.brokeros.risk.tradingaccount.domain.AuthoritySnapshotRef;
import com.brokeros.risk.tradingaccount.domain.EligibilityDecision;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountReferenceEligibility;
import org.junit.jupiter.api.Test;

class RiskCaseReferenceAdapterTests {

    private static final Instant NOW = Instant.parse("2026-09-02T00:00:00Z");
    private static final TradingAccountRef SUBJECT =
            new TradingAccountRef("ta-60000000-0000-4000-8000-000000000001");
    private static final EvidenceRef EVIDENCE =
            new EvidenceRef("ev-61000000-0000-4000-8000-000000000001");
    private static final DecisionRef DECISION =
            new DecisionRef("dec-62000000-0000-4000-8000-000000000001");
    private static final ActionRef ACTION =
            new ActionRef("act-63000000-0000-4000-8000-000000000001");
    private static final ActionOutcomeRef OUTCOME =
            new ActionOutcomeRef("aoc-64000000-0000-4000-8000-000000000001");

    @Test
    void strictSubjectBarAcceptsOnlyEligibleAndDistinguishesBothRejections() {
        TradingAccountReferenceEligibilityService service =
                mock(TradingAccountReferenceEligibilityService.class);
        TradingAccountReferenceAdapter adapter = new TradingAccountReferenceAdapter(service);

        when(service.validateForNewRiskCaseAssociation(actor(), SUBJECT))
                .thenReturn(eligibility(EligibilityDecision.ELIGIBLE_FOR_NEW_ASSOCIATION));
        assertThatCode(() -> adapter.requireEligibleForNewCase(actor(), SUBJECT))
                .doesNotThrowAnyException();

        when(service.validateForNewRiskCaseAssociation(actor(), SUBJECT))
                .thenReturn(eligibility(EligibilityDecision.RECOGNIZED_NOT_ELIGIBLE));
        assertResultCode(() -> adapter.requireEligibleForNewCase(actor(), SUBJECT),
                ResultCode.RISK_CASE_SUBJECT_NOT_ELIGIBLE);

        when(service.validateForNewRiskCaseAssociation(actor(), SUBJECT))
                .thenReturn(new TradingAccountReferenceEligibility(
                        SUBJECT, EligibilityDecision.NOT_RECOGNIZED, null, null));
        assertResultCode(() -> adapter.requireEligibleForNewCase(actor(), SUBJECT),
                ResultCode.RISK_CASE_REFERENCE_NOT_FOUND);
    }

    @Test
    void everyShippedProvenanceAdapterMapsNotFoundWithoutAcceptingOpaqueStrings() {
        EvidenceProvenanceQueryService evidence = mock(EvidenceProvenanceQueryService.class);
        when(evidence.confirmProvenance(actor(), EVIDENCE))
                .thenReturn(EvidenceProvenanceView.notFound(EVIDENCE));
        assertResultCode(() -> new EvidenceReferenceAdapter(evidence)
                        .requireRecognized(actor(), EVIDENCE),
                ResultCode.RISK_CASE_REFERENCE_NOT_FOUND);

        DecisionProvenanceQueryService decision = mock(DecisionProvenanceQueryService.class);
        when(decision.confirmProvenance(actor(), DECISION))
                .thenReturn(DecisionProvenanceView.notFound(DECISION));
        assertResultCode(() -> new DecisionReferenceAdapter(decision)
                        .requireRecognized(actor(), DECISION),
                ResultCode.RISK_CASE_REFERENCE_NOT_FOUND);

        ActionProvenanceQueryService action = mock(ActionProvenanceQueryService.class);
        when(action.confirmProvenance(actor(), ACTION))
                .thenReturn(ActionProvenanceView.notFound(ACTION));
        assertResultCode(() -> new ActionReferenceAdapter(action)
                        .requireRecognized(actor(), ACTION),
                ResultCode.RISK_CASE_REFERENCE_NOT_FOUND);

        ActionOutcomeProvenanceQueryService outcome =
                mock(ActionOutcomeProvenanceQueryService.class);
        when(outcome.confirmProvenance(actor(), OUTCOME))
                .thenReturn(ActionOutcomeProvenanceView.notFound(OUTCOME));
        assertResultCode(() -> new ActionOutcomeReferenceAdapter(outcome)
                        .requireRecognized(actor(), OUTCOME),
                ResultCode.RISK_CASE_REFERENCE_NOT_FOUND);
    }

    @Test
    void allProviderAuthorityFailuresMapToOneFailClosedUnavailableCode() {
        TradingAccountReferenceEligibilityService subject =
                mock(TradingAccountReferenceEligibilityService.class);
        when(subject.validateForNewRiskCaseAssociation(actor(), SUBJECT))
                .thenThrow(new TradingAccountAuthorityUnavailableException());
        assertResultCode(() -> new TradingAccountReferenceAdapter(subject)
                        .requireEligibleForNewCase(actor(), SUBJECT),
                ResultCode.RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE);

        EvidenceProvenanceQueryService evidence = mock(EvidenceProvenanceQueryService.class);
        when(evidence.confirmProvenance(actor(), EVIDENCE))
                .thenThrow(new EvidenceAuthorityUnavailableException());
        assertResultCode(() -> new EvidenceReferenceAdapter(evidence)
                        .requireRecognized(actor(), EVIDENCE),
                ResultCode.RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE);

        DecisionProvenanceQueryService decision = mock(DecisionProvenanceQueryService.class);
        when(decision.confirmProvenance(actor(), DECISION))
                .thenThrow(new DecisionAuthorityUnavailableException());
        assertResultCode(() -> new DecisionReferenceAdapter(decision)
                        .requireRecognized(actor(), DECISION),
                ResultCode.RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE);

        ActionProvenanceQueryService action = mock(ActionProvenanceQueryService.class);
        when(action.confirmProvenance(actor(), ACTION))
                .thenThrow(new ActionAuthorityUnavailableException());
        assertResultCode(() -> new ActionReferenceAdapter(action)
                        .requireRecognized(actor(), ACTION),
                ResultCode.RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE);

        ActionOutcomeProvenanceQueryService outcome =
                mock(ActionOutcomeProvenanceQueryService.class);
        when(outcome.confirmProvenance(actor(), OUTCOME))
                .thenThrow(new ActionOutcomeAuthorityUnavailableException());
        assertResultCode(() -> new ActionOutcomeReferenceAdapter(outcome)
                        .requireRecognized(actor(), OUTCOME),
                ResultCode.RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE);
    }

    private TradingAccountReferenceEligibility eligibility(EligibilityDecision decision) {
        return new TradingAccountReferenceEligibility(
                SUBJECT, decision,
                new AuthoritySnapshotRef("tasv1-" + "1".repeat(64)),
                new AuthorityProvenanceRef("tapv1-" + "2".repeat(64)));
    }

    private ActorContext actor() {
        return new ActorContext(
                new ActorRef("50000000-0000-4000-8000-000000000001"),
                ActorType.HUMAN,
                new ExternalPrincipalKey(
                        "urn:brokeros:risk:q008-test", "operator", ActorType.HUMAN),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.fromString("50000000-0000-4000-8000-000000000099"),
                null, null);
    }

    private void assertResultCode(ThrowingCall call, ResultCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(RiskCaseException.class)
                .extracting(error -> ((RiskCaseException) error).getResultCode())
                .isEqualTo(expected);
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run();
    }
}
