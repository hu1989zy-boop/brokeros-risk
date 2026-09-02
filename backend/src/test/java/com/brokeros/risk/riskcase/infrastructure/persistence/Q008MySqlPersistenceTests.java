package com.brokeros.risk.riskcase.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.audit.infrastructure.persistence.JdbcAuditRecordWriter;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.riskcase.application.CreateRiskCaseCommand;
import com.brokeros.risk.riskcase.application.RiskCaseAssociationService;
import com.brokeros.risk.riskcase.application.RiskCaseAuditFactory;
import com.brokeros.risk.riskcase.application.RiskCaseCapabilities;
import com.brokeros.risk.riskcase.application.RiskCaseCommandService;
import com.brokeros.risk.riskcase.application.RiskCaseCreationService;
import com.brokeros.risk.riskcase.application.RiskCaseException;
import com.brokeros.risk.riskcase.application.RiskCaseFingerprintFactory;
import com.brokeros.risk.riskcase.application.RiskCaseHistoryCursor;
import com.brokeros.risk.riskcase.application.RiskCaseMetricOperation;
import com.brokeros.risk.riskcase.application.RiskCaseQueryService;
import com.brokeros.risk.riskcase.application.RiskCaseResolutionService;
import com.brokeros.risk.riskcase.application.port.ActionOutcomeReferenceQuery;
import com.brokeros.risk.riskcase.application.port.ActionReferenceQuery;
import com.brokeros.risk.riskcase.application.port.DecisionReferenceQuery;
import com.brokeros.risk.riskcase.application.port.EvidenceReferenceQuery;
import com.brokeros.risk.riskcase.application.port.RiskCaseMetricsPort;
import com.brokeros.risk.riskcase.application.port.TradingAccountReferenceQuery;
import com.brokeros.risk.riskcase.domain.CaseNumber;
import com.brokeros.risk.riskcase.domain.CaseNumberGenerator;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named = "Q008_MYSQL_TEST_URL", matches = ".+")
class Q008MySqlPersistenceTests {

    private static final Instant NOW = Instant.parse("2026-09-02T00:00:00Z");
    private static final ActorRef ACTOR =
            new ActorRef("50000000-0000-4000-8000-000000000001");
    private static final DecisionRef DECISION =
            new DecisionRef("dec-70000000-0000-4000-8000-000000000001");

    private DataSource dataSource;
    private JdbcTemplate jdbc;
    private JdbcRiskCaseRepository repository;
    private DataSourceTransactionManager transactionManager;

    @BeforeEach
    void migrateDisposableDatabase() {
        dataSource = dataSource();
        Flyway flyway = Flyway.configure().dataSource(dataSource)
                .cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
        jdbc = new JdbcTemplate(dataSource);
        repository = new JdbcRiskCaseRepository(jdbc);
        transactionManager = new DataSourceTransactionManager(dataSource);
    }

    @Test
    void createAndExactReplayPersistOneRootTransitionAndAudit() {
        CreateRiskCaseCommand command = manualCommand("idempotency-key-0001");
        RiskCaseCreationService service = creationService(caseNumbers(1));

        var first = service.create(actorContext(), command);
        var replay = service.create(actorContext(), command);

        assertThat(replay).isEqualTo(first);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM risk_case", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM risk_case_transition_history", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_record", Integer.class))
                .isEqualTo(1);
        assertThat(repository.findByCaseNumber(first.caseNumber())).isPresent();
    }

    @Test
    void historyWriteFailureRollsBackRootAndAudit() {
        jdbc.execute("""
                CREATE TRIGGER q008_force_history_failure
                BEFORE INSERT ON risk_case_transition_history
                FOR EACH ROW SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'forced risk case history failure'
                """);
        try {
            assertThatThrownBy(() -> creationService(caseNumbers(1)).create(
                    actorContext(), manualCommand("idempotency-key-0002")))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("DROP TRIGGER q008_force_history_failure");
        }
        assertEmptyCreationTables();
    }

    @Test
    void auditWriteFailureRollsBackRootAndHistory() {
        jdbc.execute("""
                CREATE TRIGGER q008_force_audit_failure
                BEFORE INSERT ON audit_record
                FOR EACH ROW SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'forced risk case audit failure'
                """);
        try {
            assertThatThrownBy(() -> creationService(caseNumbers(1)).create(
                    actorContext(), manualCommand("idempotency-key-0003")))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("DROP TRIGGER q008_force_audit_failure");
        }
        assertEmptyCreationTables();
    }

    @Test
    void concurrentWritersOnOneVersionProduceOneCommitAndNoLoserHistoryOrAudit()
            throws Exception {
        var created = creationService(caseNumbers(1)).create(
                actorContext(), manualCommand("idempotency-key-0004"));
        RiskCaseCommandService service = commandService();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> assignmentOutcome(
                    service, created.caseNumber().value(), actor(2), start));
            var second = executor.submit(() -> assignmentOutcome(
                    service, created.caseNumber().value(), actor(3), start));
            start.countDown();
            assertThat(Set.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder(
                            "SUCCESS", ResultCode.RISK_CASE_VERSION_CONFLICT.name());
        }
        assertThat(jdbc.queryForObject(
                "SELECT version FROM risk_case WHERE case_number = ?",
                Long.class, created.caseNumber().value())).isEqualTo(2L);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM risk_case_assignment_history", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_record", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void concurrentDecisionDrivenCreationElectsOnePrimaryCaseAndRollsBackLoser()
            throws Exception {
        RiskCaseCreationService firstService = creationService(caseNumbers(1));
        RiskCaseCreationService secondService = creationService(caseNumbers(2));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> decisionCreateOutcome(
                    firstService, "idempotency-key-0005", start));
            var second = executor.submit(() -> decisionCreateOutcome(
                    secondService, "idempotency-key-0006", start));
            start.countDown();
            assertThat(Set.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder("SUCCESS",
                            ResultCode.RISK_CASE_PRIMARY_DECISION_CONFLICT.name());
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM risk_case", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM risk_case_decision_association", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM risk_case_decision_selection_history", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_record", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void failedReadAuditPreventsReturningCaseDetail() {
        var created = creationService(caseNumbers(1)).create(
                actorContext(), manualCommand("idempotency-key-0007"));
        jdbc.execute("""
                CREATE TRIGGER q008_force_read_audit_failure
                BEFORE INSERT ON audit_record
                FOR EACH ROW SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'forced risk case read audit failure'
                """);
        RiskCaseQueryService query = new RiskCaseQueryService(
                authorizationGuard(), repository, new JdbcAuditRecordWriter(jdbc),
                new RiskCaseAuditFactory(), NOOP_METRICS, fixedClock(), transactionManager);
        try {
            assertThatThrownBy(() -> query.detail(
                    actorContext(), created.caseNumber().value()))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("DROP TRIGGER q008_force_read_audit_failure");
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_record", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void concurrentResolveOnOneVersionWritesOneResolutionHistoryAndAudit()
            throws Exception {
        var review = preparedReviewCase("idempotency-key-0008");
        RiskCaseResolutionService service = resolutionService();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> resolveOutcome(
                    service, review.caseNumber().value(), start));
            var second = executor.submit(() -> resolveOutcome(
                    service, review.caseNumber().value(), start));
            start.countDown();
            assertThat(first.get()).isNotEqualTo(second.get());
            assertThat(Set.of(first.get(), second.get())).contains("SUCCESS");
        }
        assertThat(jdbc.queryForObject(
                "SELECT version FROM risk_case WHERE case_number = ?",
                Long.class, review.caseNumber().value())).isEqualTo(5L);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM risk_case_resolution_history", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM risk_case_transition_history WHERE operation_code = 'RESOLVE'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_record WHERE operation_code = 'RISK_CASE_RESOLVED'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentCloseAndResumeOnResolvedVersionLeaveExactlyOneTerminalHistory()
            throws Exception {
        var review = preparedReviewCase("idempotency-key-0009");
        var resolved = resolutionService().resolve(
                actorContext(), review.caseNumber().value(), "NO_RISK", "resolved",
                Set.of(), Set.of(), 4).riskCase();
        RiskCaseCommandService service = commandService();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var close = executor.submit(() -> commandOutcome(start, () -> service.close(
                    actorContext(), resolved.caseNumber().value(), "close", 5)));
            var resume = executor.submit(() -> commandOutcome(start, () ->
                    service.resumeResolved(actorContext(), resolved.caseNumber().value(),
                            "resume", null, 5)));
            start.countDown();
            assertThat(close.get()).isNotEqualTo(resume.get());
            assertThat(Set.of(close.get(), resume.get())).contains("SUCCESS");
        }
        assertThat(jdbc.queryForObject(
                "SELECT version FROM risk_case WHERE case_number = ?",
                Long.class, resolved.caseNumber().value())).isEqualTo(6L);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM risk_case_transition_history
                WHERE operation_code IN ('CLOSE', 'RESUME_RESOLVED')
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM audit_record
                WHERE operation_code IN ('RISK_CASE_CLOSED', 'RISK_CASE_RESOLUTION_REOPENED')
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentReopenOnClosedVersionStartsOnlyOneNewCycle() throws Exception {
        var review = preparedReviewCase("idempotency-key-0010");
        var resolved = resolutionService().resolve(
                actorContext(), review.caseNumber().value(), "NO_RISK", "resolved",
                Set.of(), Set.of(), 4).riskCase();
        var closed = commandService().close(
                actorContext(), resolved.caseNumber().value(), "close", 5);
        RiskCaseCommandService service = commandService();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> commandOutcome(start, () ->
                    service.reopenClosed(actorContext(), closed.caseNumber().value(),
                            "reopen", null, 6)));
            var second = executor.submit(() -> commandOutcome(start, () ->
                    service.reopenClosed(actorContext(), closed.caseNumber().value(),
                            "reopen", null, 6)));
            start.countDown();
            assertThat(first.get()).isNotEqualTo(second.get());
            assertThat(Set.of(first.get(), second.get())).contains("SUCCESS");
        }
        assertThat(jdbc.queryForObject(
                "SELECT version FROM risk_case WHERE case_number = ?",
                Long.class, closed.caseNumber().value())).isEqualTo(7L);
        assertThat(jdbc.queryForObject(
                "SELECT current_cycle_no FROM risk_case WHERE case_number = ?",
                Integer.class, closed.caseNumber().value())).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM risk_case_transition_history
                WHERE operation_code = 'REOPEN_CLOSED'
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM risk_case_resolution_history", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void concurrentDuplicateEvidenceAssociationAppendsOnlyOneImmutableEvent()
            throws Exception {
        var created = creationService(caseNumbers(1)).create(
                actorContext(), manualCommand("idempotency-key-0011"));
        RiskCaseAssociationService service = associationService();
        CountDownLatch start = new CountDownLatch(1);
        String evidenceRef = "ev-81000000-0000-4000-8000-000000000001";
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> associationOutcome(start, () ->
                    service.associateEvidence(actorContext(), created.caseNumber().value(),
                            evidenceRef, "attach", "MANUAL", 1)));
            var second = executor.submit(() -> associationOutcome(start, () ->
                    service.associateEvidence(actorContext(), created.caseNumber().value(),
                            evidenceRef, "attach", "MANUAL", 1)));
            start.countDown();
            assertThat(first.get()).isNotEqualTo(second.get());
            assertThat(Set.of(first.get(), second.get())).contains("SUCCESS");
        }
        assertThat(jdbc.queryForObject(
                "SELECT version FROM risk_case WHERE case_number = ?",
                Long.class, created.caseNumber().value())).isEqualTo(2L);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM risk_case_evidence_association_history",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM audit_record
                WHERE operation_code = 'RISK_CASE_EVIDENCE_ASSOCIATED'
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void historyPaginationUsesDeterministicVersionRankAndRowOrder() {
        var review = preparedReviewCase("idempotency-key-0012");

        var first = repository.findHistory(
                review.id(), new RiskCaseHistoryCursor(0, 0, 0), 3);
        var last = first.getLast();
        var second = repository.findHistory(review.id(), new RiskCaseHistoryCursor(
                last.caseVersion(), last.eventRank(), last.rowId()), 3);

        assertThat(first).extracting(entry -> entry.caseVersion())
                .containsExactly(1L, 2L, 3L);
        assertThat(second).extracting(entry -> entry.caseVersion())
                .containsExactly(4L, 4L);
        assertThat(second).extracting(entry -> entry.eventRank())
                .containsExactly(5, 6);
        assertThat(second).extracting(entry -> entry.eventType())
                .containsExactly("DECISION_ASSOCIATED", "DECISION_SELECTED");
    }

    @Test
    void appendOnlyNotesEvidenceActionsAndTwoResolutionCyclesRemainImmutable() {
        String key = "idempotency-key-0013";
        String evidenceOne = "ev-81000000-0000-4000-8000-000000000001";
        String evidenceTwo = "ev-81000000-0000-4000-8000-000000000002";
        String action = "act-80000000-0000-4000-8000-000000000001";
        String outcome = "aoc-90000000-0000-4000-8000-000000000001";
        String secondDecision = "dec-70000000-0000-4000-8000-000000000002";
        var created = creationService(caseNumbers(1)).create(actorContext(), manualCommand(key));
        RiskCaseAssociationService associations = associationService();
        var note = associations.addNote(
                actorContext(), created.caseNumber().value(), "original note", 1);
        associations.correctNote(actorContext(), created.caseNumber().value(),
                note.noteRef().value(), "corrected note", 2);
        var evidence = associations.associateEvidence(actorContext(),
                created.caseNumber().value(), evidenceOne, "attach", "MANUAL", 3);
        associations.changeEvidenceDisposition(actorContext(), created.caseNumber().value(),
                evidence.eventRef().value(), "SUPERSEDED", evidenceTwo,
                "newer evidence", "MANUAL", 4);
        commandService().changeAssignment(actorContext(), created.caseNumber().value(),
                actor(2), "assign", 5);
        commandService().beginReview(
                actorContext(), created.caseNumber().value(), "begin", 6);
        associations.associateDecision(actorContext(), created.caseNumber().value(),
                DECISION.value(), "decision one", 7);
        associations.associateAction(actorContext(), created.caseNumber().value(),
                action, "action", 8);
        commandService().markActionRequired(
                actorContext(), created.caseNumber().value(), "required", 9);
        associations.recordActionOutcomeReference(actorContext(),
                created.caseNumber().value(), action, outcome, "outcome", 10);
        var firstResolution = resolutionService().resolve(actorContext(),
                created.caseNumber().value(), "RISK_CONFIRMED_ACTION_COMPLETED",
                "cycle one", Set.of(evidenceTwo), Set.of(action), 11).riskCase();
        var closed = commandService().close(
                actorContext(), created.caseNumber().value(), "close", 12);
        var reopened = commandService().reopenClosed(
                actorContext(), closed.caseNumber().value(), "reopen", null, 13);
        associations.associateDecision(actorContext(), reopened.caseNumber().value(),
                secondDecision, "decision two", 14);
        resolutionService().resolve(actorContext(), reopened.caseNumber().value(),
                "NO_RISK", "cycle two", Set.of(evidenceTwo), Set.of(), 15);

        assertThat(firstResolution.currentCycle().value()).isEqualTo(1);
        assertThat(jdbc.queryForList(
                "SELECT content FROM risk_case_note ORDER BY case_version", String.class))
                .containsExactly("original note", "corrected note");
        assertThat(jdbc.queryForList("""
                SELECT event_type FROM risk_case_evidence_association_history
                ORDER BY case_version
                """, String.class)).containsExactly("ATTACHED", "SUPERSEDED");
        assertThat(jdbc.queryForList("""
                SELECT outcome_ref FROM risk_case_action_association_history
                ORDER BY case_version
                """, String.class)).containsExactly(null, outcome);
        assertThat(jdbc.queryForList("""
                SELECT cycle_no FROM risk_case_resolution_history ORDER BY cycle_no
                """, Integer.class)).containsExactly(1, 2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM risk_case_resolution_evidence_reference",
                Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM risk_case_resolution_action_reference",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT version FROM risk_case WHERE case_number = ?",
                Long.class, created.caseNumber().value())).isEqualTo(16L);
    }

    private String assignmentOutcome(
            RiskCaseCommandService service,
            String caseNumber,
            String assignee,
            CountDownLatch start) throws InterruptedException {
        start.await();
        try {
            service.changeAssignment(actorContext(), caseNumber, assignee, "assign", 1);
            return "SUCCESS";
        } catch (RiskCaseException exception) {
            return exception.getResultCode().name();
        }
    }

    private String decisionCreateOutcome(
            RiskCaseCreationService service,
            String key,
            CountDownLatch start) throws InterruptedException {
        start.await();
        try {
            service.create(actorContext(), new CreateRiskCaseCommand(
                    "DECISION_DRIVEN", "TRADING_ACCOUNT", subject(1),
                    "decision intake", "HIGH", DECISION.value(), key));
            return "SUCCESS";
        } catch (RiskCaseException exception) {
            return exception.getResultCode().name();
        }
    }

    private com.brokeros.risk.riskcase.domain.RiskCaseSnapshot preparedReviewCase(
            String key) {
        var created = creationService(caseNumbers(1)).create(actorContext(), manualCommand(key));
        var assigned = commandService().changeAssignment(
                actorContext(), created.caseNumber().value(), actor(2), "assign", 1);
        var review = commandService().beginReview(
                actorContext(), assigned.caseNumber().value(), "begin", 2);
        return associationService().associateDecision(
                actorContext(), review.caseNumber().value(), DECISION.value(),
                "associate decision", 3);
    }

    private String resolveOutcome(
            RiskCaseResolutionService service,
            String caseNumber,
            CountDownLatch start) throws InterruptedException {
        return commandOutcome(start, () -> service.resolve(
                actorContext(), caseNumber, "NO_RISK", "resolved",
                Set.of(), Set.of(), 4));
    }

    private String commandOutcome(CountDownLatch start, CommandCall call)
            throws InterruptedException {
        start.await();
        try {
            call.run();
            return "SUCCESS";
        } catch (RiskCaseException exception) {
            return exception.getResultCode().name();
        }
    }

    private String associationOutcome(CountDownLatch start, CommandCall call)
            throws InterruptedException {
        return commandOutcome(start, call);
    }

    private RiskCaseCreationService creationService(CaseNumberGenerator generator) {
        TradingAccountReferenceQuery subjectQuery = (context, subjectRef) -> { };
        DecisionReferenceQuery decisionQuery = (context, decisionRef) ->
                new DecisionReferenceQuery.RecognizedDecision(decisionRef, Set.of());
        return new RiskCaseCreationService(
                authorizationGuard(), subjectQuery, decisionQuery, repository,
                new JdbcAuditRecordWriter(jdbc), generator,
                new RiskCaseFingerprintFactory(), new RiskCaseAuditFactory(),
                NOOP_METRICS, fixedClock(), transactionManager);
    }

    private RiskCaseCommandService commandService() {
        return new RiskCaseCommandService(
                authorizationGuard(), repository, new JdbcAuditRecordWriter(jdbc),
                new RiskCaseAuditFactory(), NOOP_METRICS,
                fixedClock(), transactionManager);
    }

    private RiskCaseAssociationService associationService() {
        EvidenceReferenceQuery evidence = (context, reference) -> { };
        DecisionReferenceQuery decision = (context, reference) ->
                new DecisionReferenceQuery.RecognizedDecision(reference, Set.of());
        ActionReferenceQuery action = (context, reference) ->
                new ActionReferenceQuery.RecognizedAction(reference, DECISION);
        ActionOutcomeReferenceQuery outcome = (context, reference) ->
                new ActionOutcomeReferenceQuery.RecognizedActionOutcome(
                        reference, new com.brokeros.risk.action.domain.ActionRef(
                                "act-80000000-0000-4000-8000-000000000001"));
        return new RiskCaseAssociationService(
                authorizationGuard(), evidence, decision, action, outcome,
                repository, new JdbcAuditRecordWriter(jdbc), new RiskCaseAuditFactory(),
                NOOP_METRICS, fixedClock(), transactionManager);
    }

    private RiskCaseResolutionService resolutionService() {
        DecisionReferenceQuery decision = (context, reference) ->
                new DecisionReferenceQuery.RecognizedDecision(reference, Set.of());
        EvidenceReferenceQuery evidence = (context, reference) -> { };
        ActionReferenceQuery action = (context, reference) ->
                new ActionReferenceQuery.RecognizedAction(reference, DECISION);
        return new RiskCaseResolutionService(
                authorizationGuard(), decision, evidence, action, repository,
                new JdbcAuditRecordWriter(jdbc), new RiskCaseAuditFactory(),
                NOOP_METRICS, fixedClock(), transactionManager);
    }

    private AuthorizationGuard authorizationGuard() {
        return new AuthorizationGuard((context, capability) -> AuthorizationDecision.allow(
                context.actorRef(), capability, NOW, 1, 1));
    }

    private ActorContext actorContext() {
        return new ActorContext(
                ACTOR, ActorType.HUMAN,
                new ExternalPrincipalKey(
                        "urn:brokeros:risk:q008-test", "operator", ActorType.HUMAN),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.fromString("50000000-0000-4000-8000-000000000099"),
                "q008-request", "0123456789abcdef0123456789abcdef");
    }

    private CreateRiskCaseCommand manualCommand(String key) {
        return new CreateRiskCaseCommand(
                "MANUAL", "TRADING_ACCOUNT", subject(1), "manual intake",
                "NORMAL", null, key);
    }

    private CaseNumberGenerator caseNumbers(int initial) {
        AtomicInteger sequence = new AtomicInteger(initial);
        return () -> new CaseNumber("RC-a0000000-0000-4000-8000-"
                + String.format("%012d", sequence.getAndIncrement()));
    }

    private Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    private void assertEmptyCreationTables() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM risk_case", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM risk_case_transition_history", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_record", Integer.class))
                .isZero();
    }

    private String actor(int value) {
        return "50000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private String subject(int value) {
        return "ta-60000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private DataSource dataSource() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        source.setUrl(required("Q008_MYSQL_TEST_URL"));
        source.setUsername(required("Q008_MYSQL_TEST_USERNAME"));
        source.setPassword(required("Q008_MYSQL_TEST_PASSWORD"));
        return source;
    }

    private String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private static final RiskCaseMetricsPort NOOP_METRICS = new RiskCaseMetricsPort() {
        @Override
        public void recordSuccess(RiskCaseMetricOperation operation) {
        }

        @Override
        public void recordConflict(String category) {
        }

        @Override
        public void recordAuthorizationDenied(Capability capability) {
        }

        @Override
        public void recordDuration(RiskCaseMetricOperation operation, Duration duration) {
        }
    };

    @FunctionalInterface
    private interface CommandCall {
        Object run();
    }
}
