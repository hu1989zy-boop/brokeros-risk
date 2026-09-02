package com.brokeros.risk.riskcase.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.junit.jupiter.api.Test;

class RiskCaseDomainTests {

    private static final Instant NOW = Instant.parse("2026-09-02T00:00:00Z");
    private static final ActorRef ACTOR =
            new ActorRef("10000000-0000-4000-8000-000000000001");
    private static final ActorRef ASSIGNEE =
            new ActorRef("10000000-0000-4000-8000-000000000002");
    private static final DecisionRef DECISION =
            new DecisionRef("dec-20000000-0000-4000-8000-000000000001");

    @Test
    void caseNumberRequiresCanonicalUpperNamespaceAndLowercaseUuidV4() {
        assertThat(new CaseNumber("RC-30000000-0000-4000-8000-000000000001").value())
                .startsWith("RC-");
        assertThatThrownBy(() -> new CaseNumber("rc-30000000-0000-4000-8000-000000000001"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CaseNumber("RC-30000000-0000-1000-8000-000000000001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void manualAndDecisionDrivenCreationPreserveConditionalIntake() {
        RiskCase manual = manual();
        RiskCase driven = RiskCase.openDecisionDriven(
                caseNumber(2), subject(), "decision intake", RiskCasePriority.HIGH,
                DECISION, ACTOR, NOW);

        assertThat(manual.snapshot().intakeSource()).isEqualTo(CaseIntakeSource.MANUAL);
        assertThat(manual.snapshot().currentDecisionRef()).isNull();
        assertThat(driven.snapshot().intakeSource())
                .isEqualTo(CaseIntakeSource.DECISION_DRIVEN);
        assertThat(driven.snapshot().currentDecisionRef()).isEqualTo(DECISION);
        assertThat(driven.snapshot().status()).isEqualTo(RiskCaseStatus.OPEN);
        assertThat(driven.snapshot().version()).isEqualTo(1);
    }

    @Test
    void openToReviewRequiresAssignmentAndUsesNamedOperations() {
        RiskCase riskCase = persistedManual();

        assertThatThrownBy(() -> riskCase.beginReview(ACTOR, "start", NOW, 1))
                .isInstanceOf(RiskCaseDomainException.class)
                .extracting(error -> ((RiskCaseDomainException) error).error())
                .isEqualTo(RiskCaseDomainError.INVARIANT_VIOLATION);

        riskCase.assign(ASSIGNEE, ACTOR, "take ownership", NOW, 1);
        TransitionRecord transition = riskCase.beginReview(ACTOR, "start", NOW, 2);

        assertThat(transition.fromStatus()).isEqualTo(RiskCaseStatus.OPEN);
        assertThat(transition.toStatus()).isEqualTo(RiskCaseStatus.IN_REVIEW);
        assertThat(riskCase.snapshot().version()).isEqualTo(3);
    }

    @Test
    void reviewCanResolveDirectlyWithCurrentDecision() {
        RiskCase riskCase = reviewCase();
        riskCase.associateDecision(DECISION, ACTOR, "select", NOW, 3);

        TransitionRecord resolution = riskCase.resolve(
                true, ACTOR, "no risk", NOW, 4);

        assertThat(resolution.toStatus()).isEqualTo(RiskCaseStatus.RESOLVED);
        assertThat(riskCase.snapshot().currentCycle().value()).isEqualTo(1);
    }

    @Test
    void actionRequiredPathRequiresDecisionActionAndOutcomeBeforeResolution() {
        RiskCase riskCase = reviewCase();
        riskCase.associateDecision(DECISION, ACTOR, "select", NOW, 3);

        assertThatThrownBy(() -> riskCase.markActionRequired(
                false, ACTOR, "act", NOW, 4))
                .isInstanceOf(RiskCaseDomainException.class);

        riskCase.associateAction(true, ACTOR, NOW, 4);
        riskCase.markActionRequired(true, ACTOR, "act", NOW, 5);
        assertThatThrownBy(() -> riskCase.resolve(false, ACTOR, "done", NOW, 6))
                .isInstanceOf(RiskCaseDomainException.class);
        riskCase.recordActionOutcomeReference(true, true, ACTOR, NOW, 6);
        riskCase.resolve(true, ACTOR, "done", NOW, 7);

        assertThat(riskCase.snapshot().status()).isEqualTo(RiskCaseStatus.RESOLVED);
    }

    @Test
    void actionRequiredCanReturnToReviewWithoutStartingNewCycle() {
        RiskCase riskCase = actionRequiredCase();

        riskCase.returnToReview(ACTOR, "new evidence", NOW, 6);

        assertThat(riskCase.snapshot().status()).isEqualTo(RiskCaseStatus.IN_REVIEW);
        assertThat(riskCase.snapshot().currentCycle().value()).isEqualTo(1);
        assertThat(riskCase.snapshot().currentDecisionRef()).isEqualTo(DECISION);
    }

    @Test
    void closeAndExceptionalReopenStartNewCycleAndClearDecision() {
        RiskCase riskCase = resolvedCase();
        riskCase.close(ACTOR, "administrative close", NOW, 5);

        TransitionRecord reopened = riskCase.reopenClosedCase(
                null, ACTOR, "new concern", NOW, 6);

        assertThat(reopened.operation()).isEqualTo(RiskCaseTransitionOperation.REOPEN_CLOSED);
        assertThat(riskCase.snapshot().status()).isEqualTo(RiskCaseStatus.IN_REVIEW);
        assertThat(riskCase.snapshot().currentCycle().value()).isEqualTo(2);
        assertThat(riskCase.snapshot().currentDecisionRef()).isNull();
    }

    @Test
    void resolvedCaseCanResumeAndSuppliedAssignmentIsCaptured() {
        RiskCase riskCase = resolvedCase();
        ActorRef replacement = new ActorRef("10000000-0000-4000-8000-000000000003");

        riskCase.resumeResolvedCase(replacement, ACTOR, "resume", NOW, 5);

        assertThat(riskCase.snapshot().assignment().assignee()).isEqualTo(replacement);
        assertThat(riskCase.snapshot().currentCycle().value()).isEqualTo(2);
    }

    @Test
    void cancellationIsTerminalFromEveryApprovedActiveSource() {
        RiskCase open = persistedManual();
        open.cancel(ACTOR, "duplicate", NOW, 1);
        assertTerminal(open, 2);

        RiskCase review = reviewCase();
        review.cancel(ACTOR, "invalid", NOW, 3);
        assertTerminal(review, 4);

        RiskCase actionRequired = actionRequiredCase();
        actionRequired.cancel(ACTOR, "invalid", NOW, 6);
        assertTerminal(actionRequired, 7);
    }

    @Test
    void unassignIsAllowedOnlyWhileOpen() {
        RiskCase riskCase = persistedManual();
        riskCase.assign(ASSIGNEE, ACTOR, "assign", NOW, 1);
        riskCase.unassign(ACTOR, "return", NOW, 2);
        assertThat(riskCase.snapshot().assignment()).isNull();

        riskCase.assign(ASSIGNEE, ACTOR, "assign", NOW, 3);
        riskCase.beginReview(ACTOR, "begin", NOW, 4);
        assertThatThrownBy(() -> riskCase.unassign(ACTOR, "not allowed", NOW, 5))
                .isInstanceOf(RiskCaseDomainException.class)
                .extracting(error -> ((RiskCaseDomainException) error).error())
                .isEqualTo(RiskCaseDomainError.INVALID_TRANSITION);
    }

    @Test
    void staleVersionIsRejectedBeforeMutation() {
        RiskCase riskCase = persistedManual();
        RiskCaseSnapshot before = riskCase.snapshot();

        assertThatThrownBy(() -> riskCase.assign(
                ASSIGNEE, ACTOR, "assign", NOW, 99))
                .isInstanceOf(RiskCaseDomainException.class)
                .extracting(error -> ((RiskCaseDomainException) error).error())
                .isEqualTo(RiskCaseDomainError.VERSION_CONFLICT);
        assertThat(riskCase.snapshot()).isEqualTo(before);
    }

    @Test
    void priorityMustChangeAndClosedOrCancelledCasesRejectIt() {
        RiskCase riskCase = persistedManual();
        assertThatThrownBy(() -> riskCase.changePriority(
                RiskCasePriority.NORMAL, ACTOR, "same", NOW, 1))
                .isInstanceOf(RiskCaseDomainException.class);
        riskCase.changePriority(RiskCasePriority.HIGH, ACTOR, "urgent", NOW, 1);
        assertThat(riskCase.snapshot().priority()).isEqualTo(RiskCasePriority.HIGH);
    }

    @Test
    void relationalAssociationOperationsEnforceApprovedStatusesAndFlags() {
        RiskCase riskCase = reviewCase();
        riskCase.associateDecision(DECISION, ACTOR, "select", NOW, 3);
        assertThatThrownBy(() -> riskCase.associateAction(false, ACTOR, NOW, 4))
                .isInstanceOf(RiskCaseDomainException.class);
        riskCase.associateAction(true, ACTOR, NOW, 4);
        riskCase.markActionRequired(true, ACTOR, "required", NOW, 5);
        assertThatThrownBy(() -> riskCase.recordActionOutcomeReference(
                true, false, ACTOR, NOW, 6))
                .isInstanceOf(RiskCaseDomainException.class);
    }

    @Test
    void resolutionRecordIsImmutableAndCycleBound() {
        ResolutionRecord record = new ResolutionRecord(
                1L, new RiskCaseId(1), new ResolutionCycleNumber(1), 5,
                ResolutionOutcome.NO_RISK, DECISION, "resolved", ACTOR, NOW);

        assertThat(record.cycle().value()).isEqualTo(1);
        assertThat(record.outcome()).isEqualTo(ResolutionOutcome.NO_RISK);
        assertThat(ResolutionOutcome.values()).containsExactly(
                ResolutionOutcome.RISK_CONFIRMED_ACTION_COMPLETED,
                ResolutionOutcome.NO_RISK,
                ResolutionOutcome.FALSE_POSITIVE,
                ResolutionOutcome.MONITORING_ONLY,
                ResolutionOutcome.NO_ACTION_REQUIRED);
    }

    @Test
    void materialOperationsIncreaseVersionExactlyOnceForDeterministicOrder() {
        RiskCase riskCase = persistedManual();
        long start = riskCase.snapshot().version();
        riskCase.associateEvidence(ACTOR, NOW, start);
        riskCase.addInvestigationNote(ACTOR, NOW, start + 1);
        riskCase.changePriority(RiskCasePriority.HIGH, ACTOR, "raise", NOW, start + 2);

        assertThat(riskCase.snapshot().version()).isEqualTo(start + 3);
    }

    private void assertTerminal(RiskCase riskCase, long version) {
        assertThat(riskCase.snapshot().status()).isEqualTo(RiskCaseStatus.CANCELLED);
        assertThatThrownBy(() -> riskCase.addInvestigationNote(ACTOR, NOW, version))
                .isInstanceOf(RiskCaseDomainException.class);
    }

    private RiskCase actionRequiredCase() {
        RiskCase riskCase = reviewCase();
        riskCase.associateDecision(DECISION, ACTOR, "select", NOW, 3);
        riskCase.associateAction(true, ACTOR, NOW, 4);
        riskCase.markActionRequired(true, ACTOR, "required", NOW, 5);
        return riskCase;
    }

    private RiskCase resolvedCase() {
        RiskCase riskCase = reviewCase();
        riskCase.associateDecision(DECISION, ACTOR, "select", NOW, 3);
        riskCase.resolve(true, ACTOR, "resolved", NOW, 4);
        return riskCase;
    }

    private RiskCase reviewCase() {
        RiskCase riskCase = persistedManual();
        riskCase.assign(ASSIGNEE, ACTOR, "assign", NOW, 1);
        riskCase.beginReview(ACTOR, "begin", NOW, 2);
        return riskCase;
    }

    private RiskCase persistedManual() {
        RiskCase riskCase = manual();
        riskCase.markPersisted(new RiskCaseId(1));
        return riskCase;
    }

    private RiskCase manual() {
        return RiskCase.openManual(caseNumber(1), subject(), "manual intake",
                RiskCasePriority.NORMAL, ACTOR, NOW);
    }

    private CaseNumber caseNumber(int suffix) {
        return new CaseNumber("RC-30000000-0000-4000-8000-00000000000" + suffix);
    }

    private TradingAccountSubjectRef subject() {
        return new TradingAccountSubjectRef(
                new TradingAccountRef("ta-40000000-0000-4000-8000-000000000001"));
    }
}
