package com.brokeros.risk.riskcase.infrastructure.persistence;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.riskcase.application.RiskCaseCreationRecord;
import com.brokeros.risk.riskcase.application.RiskCaseHistoryCursor;
import com.brokeros.risk.riskcase.application.RiskCaseHistoryEntry;
import com.brokeros.risk.riskcase.application.RiskCaseListQuery;
import com.brokeros.risk.riskcase.application.RiskCaseSummary;
import com.brokeros.risk.riskcase.application.port.RiskCaseConflictKind;
import com.brokeros.risk.riskcase.application.port.RiskCasePersistenceConflictException;
import com.brokeros.risk.riskcase.application.port.RiskCaseRepository;
import com.brokeros.risk.riskcase.domain.ActionAssociationEvent;
import com.brokeros.risk.riskcase.domain.ActionAssociationEventType;
import com.brokeros.risk.riskcase.domain.Assignment;
import com.brokeros.risk.riskcase.domain.AssignmentChangeRecord;
import com.brokeros.risk.riskcase.domain.CaseIntakeSource;
import com.brokeros.risk.riskcase.domain.CaseNumber;
import com.brokeros.risk.riskcase.domain.DecisionAssociation;
import com.brokeros.risk.riskcase.domain.DecisionSelectionRecord;
import com.brokeros.risk.riskcase.domain.EvidenceAssociationEvent;
import com.brokeros.risk.riskcase.domain.EvidenceAssociationEventRef;
import com.brokeros.risk.riskcase.domain.EvidenceAssociationEventType;
import com.brokeros.risk.riskcase.domain.InvestigationNote;
import com.brokeros.risk.riskcase.domain.InvestigationNoteRef;
import com.brokeros.risk.riskcase.domain.PriorityChangeRecord;
import com.brokeros.risk.riskcase.domain.ResolutionCycleNumber;
import com.brokeros.risk.riskcase.domain.ResolutionOutcome;
import com.brokeros.risk.riskcase.domain.ResolutionRecord;
import com.brokeros.risk.riskcase.domain.RiskCase;
import com.brokeros.risk.riskcase.domain.RiskCaseId;
import com.brokeros.risk.riskcase.domain.RiskCasePriority;
import com.brokeros.risk.riskcase.domain.RiskCaseSnapshot;
import com.brokeros.risk.riskcase.domain.RiskCaseStatus;
import com.brokeros.risk.riskcase.domain.RiskCaseTransitionOperation;
import com.brokeros.risk.riskcase.domain.TradingAccountSubjectRef;
import com.brokeros.risk.riskcase.domain.TransitionRecord;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRiskCaseRepository implements RiskCaseRepository {

    private static final String ROOT_COLUMNS = """
            id, case_number, subject_type, subject_ref, intake_source,
            intake_summary, status, priority, current_assignee_ref,
            assigned_by_ref, assigned_at, current_decision_ref,
            current_cycle_no, created_by_ref, created_at, updated_by_ref,
            updated_at, version
            """;

    private final JdbcTemplate jdbcTemplate;
    private final MySqlRiskCaseConstraintClassifier classifier =
            new MySqlRiskCaseConstraintClassifier();

    public JdbcRiskCaseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<RiskCase> findByCaseNumber(CaseNumber caseNumber) {
        return one("SELECT " + ROOT_COLUMNS + " FROM risk_case WHERE case_number = ?",
                caseNumber.value());
    }

    @Override
    public Optional<RiskCaseCreationRecord> findByCreationKey(
            ActorRef actorRef, byte[] idempotencyKeyHash) {
        List<RiskCaseCreationRecord> rows = jdbcTemplate.query(
                "SELECT " + ROOT_COLUMNS + ", creation_request_hash FROM risk_case "
                        + "WHERE created_by_ref = ? AND creation_idempotency_key_hash = ?",
                (resultSet, rowNumber) -> new RiskCaseCreationRecord(
                        mapRoot(resultSet), resultSet.getBytes("creation_request_hash")),
                actorRef.value(), idempotencyKeyHash);
        return single(rows);
    }

    @Override
    public Optional<RiskCase> findByPrimaryDecision(DecisionRef decisionRef) {
        return one("SELECT " + prefixedRootColumns("case_row") + " "
                        + "FROM risk_case_decision_association association_row "
                        + "JOIN risk_case case_row ON case_row.id = association_row.case_id "
                        + "WHERE association_row.decision_ref = ?",
                decisionRef.value());
    }

    @Override
    public RiskCase insertRoot(RiskCase riskCase, byte[] keyHash, byte[] requestHash) {
        RiskCaseSnapshot snapshot = riskCase.snapshot();
        KeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO risk_case (
                            case_number, subject_type, subject_ref, intake_source,
                            intake_summary, status, priority, current_assignee_ref,
                            assigned_by_ref, assigned_at, current_decision_ref,
                            current_cycle_no, creation_idempotency_key_hash,
                            creation_request_hash, created_by_ref, created_at,
                            updated_by_ref, updated_at, version)
                        VALUES (?, 'TRADING_ACCOUNT', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                bindRootInsert(statement, snapshot, keyHash, requestHash);
                return statement;
            }, keys);
        } catch (DataAccessException exception) {
            RiskCaseConflictKind kind = classifier.classify(exception);
            if (kind != RiskCaseConflictKind.OTHER) {
                throw new RiskCasePersistenceConflictException(kind, exception);
            }
            throw exception;
        }
        Number generated = keys.getKey();
        if (generated == null) {
            throw new IllegalStateException("risk case insert returned no identity");
        }
        riskCase.markPersisted(new RiskCaseId(generated.longValue()));
        return riskCase;
    }

    @Override
    public int updateRoot(RiskCaseSnapshot snapshot, long expectedVersion) {
        return jdbcTemplate.update("""
                UPDATE risk_case
                SET status = ?, priority = ?, current_assignee_ref = ?,
                    assigned_by_ref = ?, assigned_at = ?, current_decision_ref = ?,
                    current_cycle_no = ?, updated_by_ref = ?, updated_at = ?, version = ?
                WHERE id = ? AND version = ?
                """, snapshot.status().name(), snapshot.priority().name(),
                assignee(snapshot), assignedBy(snapshot), assignedAt(snapshot),
                value(snapshot.currentDecisionRef()), snapshot.currentCycle().value(),
                snapshot.updatedBy().value(), Timestamp.from(snapshot.updatedAt()),
                snapshot.version(), snapshot.id().value(), expectedVersion);
    }

    @Override
    public void appendTransition(TransitionRecord record) {
        jdbcTemplate.update("""
                INSERT INTO risk_case_transition_history (
                    case_id, case_version, cycle_no, operation_code, from_status,
                    to_status, reason, actor_ref, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, record.caseId().value(), record.caseVersion(), record.cycle().value(),
                record.operation().name(), name(record.fromStatus()), record.toStatus().name(),
                record.reason(), record.actorRef().value(), Timestamp.from(record.occurredAt()));
    }

    @Override
    public void appendAssignment(AssignmentChangeRecord record) {
        jdbcTemplate.update("""
                INSERT INTO risk_case_assignment_history (
                    case_id, case_version, previous_assignee_ref, new_assignee_ref,
                    assigned_by_ref, reason, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, record.caseId().value(), record.caseVersion(),
                value(record.previousAssignee()), value(record.newAssignee()),
                record.assignedBy().value(), record.reason(), Timestamp.from(record.occurredAt()));
    }

    @Override
    public void appendPriority(PriorityChangeRecord record) {
        jdbcTemplate.update("""
                INSERT INTO risk_case_priority_history (
                    case_id, case_version, previous_priority, new_priority,
                    changed_by_ref, reason, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, record.caseId().value(), record.caseVersion(),
                record.previousPriority().name(), record.newPriority().name(),
                record.changedBy().value(), record.reason(), Timestamp.from(record.occurredAt()));
    }

    @Override
    public EvidenceAssociationEvent appendEvidence(EvidenceAssociationEvent event) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO risk_case_evidence_association_history (
                        event_ref, case_id, case_version, event_type, evidence_ref,
                        prior_event_id, replacement_evidence_ref, reason, source,
                        actor_ref, occurred_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, event.eventRef().value());
            statement.setLong(2, event.caseId().value());
            statement.setLong(3, event.caseVersion());
            statement.setString(4, event.eventType().name());
            statement.setString(5, event.evidenceRef().value());
            setLong(statement, 6, event.priorEventId());
            statement.setString(7, value(event.replacementEvidenceRef()));
            statement.setString(8, event.reason());
            statement.setString(9, event.source());
            statement.setString(10, event.actorRef().value());
            statement.setTimestamp(11, Timestamp.from(event.occurredAt()));
            return statement;
        }, keys);
        return new EvidenceAssociationEvent(requireKey(keys), event.eventRef(), event.caseId(),
                event.caseVersion(), event.eventType(), event.evidenceRef(), event.priorEventId(),
                event.replacementEvidenceRef(), event.reason(), event.source(), event.actorRef(),
                event.occurredAt());
    }

    @Override
    public Optional<EvidenceAssociationEvent> findEvidenceEvent(
            RiskCaseId caseId, EvidenceAssociationEventRef eventRef) {
        List<EvidenceAssociationEvent> rows = jdbcTemplate.query("""
                SELECT id, event_ref, case_id, case_version, event_type, evidence_ref,
                       prior_event_id, replacement_evidence_ref, reason, source,
                       actor_ref, occurred_at
                FROM risk_case_evidence_association_history
                WHERE case_id = ? AND event_ref = ?
                """, (resultSet, rowNumber) -> new EvidenceAssociationEvent(
                        resultSet.getLong("id"),
                        new EvidenceAssociationEventRef(resultSet.getString("event_ref")),
                        new RiskCaseId(resultSet.getLong("case_id")),
                        resultSet.getLong("case_version"),
                        EvidenceAssociationEventType.valueOf(resultSet.getString("event_type")),
                        new EvidenceRef(resultSet.getString("evidence_ref")),
                        nullableLong(resultSet, "prior_event_id"),
                        evidenceRef(resultSet.getString("replacement_evidence_ref")),
                        resultSet.getString("reason"), resultSet.getString("source"),
                        new ActorRef(resultSet.getString("actor_ref")),
                        resultSet.getTimestamp("occurred_at").toInstant()),
                caseId.value(), eventRef.value());
        return single(rows);
    }

    @Override
    public boolean evidenceEventHasDisposition(long eventId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM risk_case_evidence_association_history WHERE prior_event_id = ?",
                Integer.class, eventId);
        return count != null && count > 0;
    }

    @Override
    public Optional<EffectiveEvidence> findEffectiveEvidence(
            RiskCaseId caseId, EvidenceRef evidenceRef) {
        return findAllEffectiveEvidence(caseId).stream()
                .filter(candidate -> candidate.evidenceRef().equals(evidenceRef))
                .findFirst();
    }

    @Override
    public List<EffectiveEvidence> findAllEffectiveEvidence(RiskCaseId caseId) {
        return jdbcTemplate.query("""
                SELECT source_id, effective_ref
                FROM (
                    SELECT attached.id AS source_id, attached.evidence_ref AS effective_ref
                    FROM risk_case_evidence_association_history attached
                    WHERE attached.case_id = ? AND attached.event_type = 'ATTACHED'
                      AND NOT EXISTS (
                          SELECT 1 FROM risk_case_evidence_association_history disposition
                          WHERE disposition.prior_event_id = attached.id)
                    UNION ALL
                    SELECT superseded.id AS source_id,
                           superseded.replacement_evidence_ref AS effective_ref
                    FROM risk_case_evidence_association_history superseded
                    WHERE superseded.case_id = ? AND superseded.event_type = 'SUPERSEDED'
                      AND NOT EXISTS (
                          SELECT 1 FROM risk_case_evidence_association_history later
                          WHERE later.prior_event_id = superseded.id)
                ) effective
                ORDER BY effective_ref
                """, (resultSet, rowNumber) -> new EffectiveEvidence(
                        resultSet.getLong("source_id"),
                        new EvidenceRef(resultSet.getString("effective_ref"))),
                caseId.value(), caseId.value());
    }

    @Override
    public List<EvidenceAssociationEvent> findAllEvidenceEvents(RiskCaseId caseId) {
        return jdbcTemplate.query("""
                SELECT id, event_ref, case_id, case_version, event_type, evidence_ref,
                       prior_event_id, replacement_evidence_ref, reason, source,
                       actor_ref, occurred_at
                FROM risk_case_evidence_association_history
                WHERE case_id = ?
                ORDER BY case_version, id
                """, (resultSet, rowNumber) -> new EvidenceAssociationEvent(
                        resultSet.getLong("id"),
                        new EvidenceAssociationEventRef(resultSet.getString("event_ref")),
                        new RiskCaseId(resultSet.getLong("case_id")),
                        resultSet.getLong("case_version"),
                        EvidenceAssociationEventType.valueOf(resultSet.getString("event_type")),
                        new EvidenceRef(resultSet.getString("evidence_ref")),
                        nullableLong(resultSet, "prior_event_id"),
                        evidenceRef(resultSet.getString("replacement_evidence_ref")),
                        resultSet.getString("reason"), resultSet.getString("source"),
                        new ActorRef(resultSet.getString("actor_ref")),
                        resultSet.getTimestamp("occurred_at").toInstant()),
                caseId.value());
    }

    @Override
    public void appendDecisionAssociation(DecisionAssociation association) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO risk_case_decision_association (
                        case_id, case_version, decision_ref, associated_by_ref,
                        reason, associated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, association.caseId().value(), association.caseVersion(),
                    association.decisionRef().value(), association.associatedBy().value(),
                    association.reason(), Timestamp.from(association.associatedAt()));
        } catch (DataAccessException exception) {
            RiskCaseConflictKind kind = classifier.classify(exception);
            if (kind == RiskCaseConflictKind.PRIMARY_DECISION) {
                throw new RiskCasePersistenceConflictException(kind, exception);
            }
            throw exception;
        }
    }

    @Override
    public List<DecisionAssociation> findAllDecisionAssociations(RiskCaseId caseId) {
        return jdbcTemplate.query("""
                SELECT id, case_id, case_version, decision_ref, associated_by_ref,
                       reason, associated_at
                FROM risk_case_decision_association
                WHERE case_id = ?
                ORDER BY case_version, id
                """, (resultSet, rowNumber) -> new DecisionAssociation(
                        resultSet.getLong("id"),
                        new RiskCaseId(resultSet.getLong("case_id")),
                        resultSet.getLong("case_version"),
                        new DecisionRef(resultSet.getString("decision_ref")),
                        new ActorRef(resultSet.getString("associated_by_ref")),
                        resultSet.getString("reason"),
                        resultSet.getTimestamp("associated_at").toInstant()),
                caseId.value());
    }

    @Override
    public void appendDecisionSelection(DecisionSelectionRecord record) {
        jdbcTemplate.update("""
                INSERT INTO risk_case_decision_selection_history (
                    case_id, case_version, previous_decision_ref, new_decision_ref,
                    selected_by_ref, reason, selected_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, record.caseId().value(), record.caseVersion(),
                value(record.previousDecisionRef()), value(record.newDecisionRef()),
                record.selectedBy().value(), record.reason(), Timestamp.from(record.selectedAt()));
    }

    @Override
    public boolean isDecisionAssociated(RiskCaseId caseId, DecisionRef decisionRef) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM risk_case_decision_association WHERE case_id = ? AND decision_ref = ?",
                Integer.class, caseId.value(), decisionRef.value());
        return count != null && count > 0;
    }

    @Override
    public ActionAssociationEvent appendAction(ActionAssociationEvent event) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO risk_case_action_association_history (
                        case_id, case_version, event_type, action_ref, decision_ref,
                        outcome_ref, prior_event_id, reason, actor_ref, occurred_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, event.caseId().value());
            statement.setLong(2, event.caseVersion());
            statement.setString(3, event.eventType().name());
            statement.setString(4, event.actionRef().value());
            statement.setString(5, event.decisionRef().value());
            statement.setString(6, value(event.outcomeRef()));
            setLong(statement, 7, event.priorEventId());
            statement.setString(8, event.reason());
            statement.setString(9, event.actorRef().value());
            statement.setTimestamp(10, Timestamp.from(event.occurredAt()));
            return statement;
        }, keys);
        return new ActionAssociationEvent(requireKey(keys), event.caseId(), event.caseVersion(),
                event.eventType(), event.actionRef(), event.decisionRef(), event.outcomeRef(),
                event.priorEventId(), event.reason(), event.actorRef(), event.occurredAt());
    }

    @Override
    public Optional<EffectiveAction> findEffectiveAction(
            RiskCaseId caseId, ActionRef actionRef) {
        return findAllEffectiveActions(caseId).stream()
                .filter(candidate -> candidate.actionRef().equals(actionRef))
                .findFirst();
    }

    @Override
    public List<EffectiveAction> findAllEffectiveActions(RiskCaseId caseId) {
        return jdbcTemplate.query("""
                SELECT event_row.id, event_row.action_ref, event_row.decision_ref,
                       event_row.outcome_ref
                FROM risk_case_action_association_history event_row
                JOIN (
                    SELECT action_ref, MAX(case_version) AS latest_version
                    FROM risk_case_action_association_history
                    WHERE case_id = ?
                    GROUP BY action_ref
                ) latest ON latest.action_ref = event_row.action_ref
                         AND latest.latest_version = event_row.case_version
                WHERE event_row.case_id = ? AND event_row.event_type <> 'WITHDRAWN'
                ORDER BY event_row.action_ref
                """, (resultSet, rowNumber) -> new EffectiveAction(
                        resultSet.getLong("id"),
                        new ActionRef(resultSet.getString("action_ref")),
                        new DecisionRef(resultSet.getString("decision_ref")),
                        actionOutcomeRef(resultSet.getString("outcome_ref"))),
                caseId.value(), caseId.value());
    }

    @Override
    public boolean hasActionForDecision(RiskCaseId caseId, DecisionRef decisionRef) {
        return findAllEffectiveActions(caseId).stream()
                .anyMatch(action -> action.decisionRef().equals(decisionRef));
    }

    @Override
    public InvestigationNote appendNote(InvestigationNote note) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO risk_case_note (
                        note_ref, case_id, case_version, content, supersedes_note_id,
                        created_by_ref, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, note.noteRef().value());
            statement.setLong(2, note.caseId().value());
            statement.setLong(3, note.caseVersion());
            statement.setString(4, note.content());
            setLong(statement, 5, note.supersedesNoteId());
            statement.setString(6, note.createdBy().value());
            statement.setTimestamp(7, Timestamp.from(note.createdAt()));
            return statement;
        }, keys);
        return new InvestigationNote(requireKey(keys), note.noteRef(), note.caseId(),
                note.caseVersion(), note.content(), note.supersedesNoteId(),
                note.createdBy(), note.createdAt());
    }

    @Override
    public Optional<InvestigationNote> findNote(
            RiskCaseId caseId, InvestigationNoteRef noteRef) {
        List<InvestigationNote> rows = jdbcTemplate.query("""
                SELECT id, note_ref, case_id, case_version, content,
                       supersedes_note_id, created_by_ref, created_at
                FROM risk_case_note
                WHERE case_id = ? AND note_ref = ?
                """, (resultSet, rowNumber) -> new InvestigationNote(
                        resultSet.getLong("id"),
                        new InvestigationNoteRef(resultSet.getString("note_ref")),
                        new RiskCaseId(resultSet.getLong("case_id")),
                        resultSet.getLong("case_version"), resultSet.getString("content"),
                        nullableLong(resultSet, "supersedes_note_id"),
                        new ActorRef(resultSet.getString("created_by_ref")),
                        resultSet.getTimestamp("created_at").toInstant()),
                caseId.value(), noteRef.value());
        return single(rows);
    }

    @Override
    public boolean noteHasCorrection(long noteId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM risk_case_note WHERE supersedes_note_id = ?",
                Integer.class, noteId);
        return count != null && count > 0;
    }

    @Override
    public ResolutionRecord appendResolution(ResolutionRecord resolution) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO risk_case_resolution_history (
                        case_id, cycle_no, case_version, outcome_code, decision_ref,
                        resolution_summary, resolved_by_ref, resolved_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, resolution.caseId().value());
            statement.setInt(2, resolution.cycle().value());
            statement.setLong(3, resolution.caseVersion());
            statement.setString(4, resolution.outcome().name());
            statement.setString(5, resolution.decisionRef().value());
            statement.setString(6, resolution.summary());
            statement.setString(7, resolution.resolvedBy().value());
            statement.setTimestamp(8, Timestamp.from(resolution.resolvedAt()));
            return statement;
        }, keys);
        return new ResolutionRecord(requireKey(keys), resolution.caseId(), resolution.cycle(),
                resolution.caseVersion(), resolution.outcome(), resolution.decisionRef(),
                resolution.summary(), resolution.resolvedBy(), resolution.resolvedAt());
    }

    @Override
    public void appendResolutionEvidence(long resolutionId, EffectiveEvidence evidence) {
        jdbcTemplate.update("""
                INSERT INTO risk_case_resolution_evidence_reference (
                    resolution_id, evidence_ref, source_association_event_id)
                VALUES (?, ?, ?)
                """, resolutionId, evidence.evidenceRef().value(), evidence.eventId());
    }

    @Override
    public void appendResolutionAction(long resolutionId, EffectiveAction action) {
        jdbcTemplate.update("""
                INSERT INTO risk_case_resolution_action_reference (
                    resolution_id, action_ref, outcome_ref, source_action_event_id)
                VALUES (?, ?, ?, ?)
                """, resolutionId, action.actionRef().value(), value(action.outcomeRef()),
                action.eventId());
    }

    @Override
    public List<RiskCaseHistoryEntry> findHistory(
            RiskCaseId caseId, RiskCaseHistoryCursor cursor, int limit) {
        return jdbcTemplate.query("""
                SELECT case_version, event_rank, row_id, event_type,
                       affected_ref, actor_ref, occurred_at
                FROM (
                    SELECT case_version, 1 event_rank, id row_id,
                           operation_code event_type, NULL affected_ref,
                           actor_ref, occurred_at
                    FROM risk_case_transition_history WHERE case_id = ?
                    UNION ALL
                    SELECT case_version, 2, id, 'ASSIGNMENT', new_assignee_ref,
                           assigned_by_ref, occurred_at
                    FROM risk_case_assignment_history WHERE case_id = ?
                    UNION ALL
                    SELECT case_version, 3, id, 'PRIORITY', new_priority,
                           changed_by_ref, occurred_at
                    FROM risk_case_priority_history WHERE case_id = ?
                    UNION ALL
                    SELECT case_version, 4, id, event_type, evidence_ref,
                           actor_ref, occurred_at
                    FROM risk_case_evidence_association_history WHERE case_id = ?
                    UNION ALL
                    SELECT case_version, 5, id, 'DECISION_ASSOCIATED', decision_ref,
                           associated_by_ref, associated_at
                    FROM risk_case_decision_association WHERE case_id = ?
                    UNION ALL
                    SELECT case_version, 6, id, 'DECISION_SELECTED', new_decision_ref,
                           selected_by_ref, selected_at
                    FROM risk_case_decision_selection_history WHERE case_id = ?
                    UNION ALL
                    SELECT case_version, 7, id, event_type, action_ref,
                           actor_ref, occurred_at
                    FROM risk_case_action_association_history WHERE case_id = ?
                    UNION ALL
                    SELECT case_version, 8, id, 'NOTE', note_ref,
                           created_by_ref, created_at
                    FROM risk_case_note WHERE case_id = ?
                    UNION ALL
                    SELECT case_version, 9, id, 'RESOLUTION', decision_ref,
                           resolved_by_ref, resolved_at
                    FROM risk_case_resolution_history WHERE case_id = ?
                ) history
                WHERE case_version > ?
                   OR (case_version = ? AND event_rank > ?)
                   OR (case_version = ? AND event_rank = ? AND row_id > ?)
                ORDER BY case_version, event_rank, row_id
                LIMIT ?
                """, (resultSet, rowNumber) -> new RiskCaseHistoryEntry(
                        resultSet.getLong("case_version"),
                        resultSet.getInt("event_rank"),
                        resultSet.getLong("row_id"),
                        resultSet.getString("event_type"),
                        resultSet.getString("affected_ref"),
                        resultSet.getString("actor_ref"),
                        resultSet.getTimestamp("occurred_at").toInstant()),
                caseId.value(), caseId.value(), caseId.value(), caseId.value(),
                caseId.value(), caseId.value(), caseId.value(), caseId.value(),
                caseId.value(), cursor.caseVersion(), cursor.caseVersion(),
                cursor.eventRank(), cursor.caseVersion(), cursor.eventRank(),
                cursor.rowId(), limit);
    }

    @Override
    public List<RiskCaseSummary> findSummaries(
            RiskCaseListQuery query, int limit, long offset) {
        if (limit < 1 || limit > 101 || offset < 0) {
            throw new IllegalArgumentException("risk case summary query is outside its bound");
        }
        StringBuilder sql = new StringBuilder("""
                SELECT case_number, subject_ref, status, priority,
                       current_assignee_ref, created_at, updated_at, version
                FROM risk_case
                WHERE 1 = 1
                """);
        List<Object> arguments = new ArrayList<>();
        if (query.status() != null) {
            sql.append(" AND status = ?");
            arguments.add(query.status().name());
        }
        if (query.priority() != null) {
            sql.append(" AND priority = ?");
            arguments.add(query.priority().name());
        }
        if (query.subjectRef() != null) {
            sql.append(" AND subject_ref = ?");
            arguments.add(query.subjectRef().value());
        }
        if (query.assigneeRef() != null) {
            sql.append(" AND current_assignee_ref = ?");
            arguments.add(query.assigneeRef().value());
        }
        sql.append(" ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?");
        arguments.add(limit);
        arguments.add(offset);
        return jdbcTemplate.query(sql.toString(),
                (resultSet, rowNumber) -> {
                    String assignee = resultSet.getString("current_assignee_ref");
                    return new RiskCaseSummary(
                            new CaseNumber(resultSet.getString("case_number")),
                            new TradingAccountRef(resultSet.getString("subject_ref")),
                            RiskCaseStatus.valueOf(resultSet.getString("status")),
                            RiskCasePriority.valueOf(resultSet.getString("priority")),
                            assignee == null ? null : new ActorRef(assignee),
                            resultSet.getTimestamp("created_at").toInstant(),
                            resultSet.getTimestamp("updated_at").toInstant(),
                            resultSet.getLong("version"));
                }, arguments.toArray());
    }

    private Optional<RiskCase> one(String sql, Object... arguments) {
        return single(jdbcTemplate.query(sql,
                (resultSet, rowNumber) -> mapRoot(resultSet), arguments));
    }

    private RiskCase mapRoot(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        String assigneeRef = resultSet.getString("current_assignee_ref");
        Assignment assignment = assigneeRef == null ? null : new Assignment(
                new ActorRef(assigneeRef),
                new ActorRef(resultSet.getString("assigned_by_ref")),
                resultSet.getTimestamp("assigned_at").toInstant());
        String decisionRef = resultSet.getString("current_decision_ref");
        RiskCaseSnapshot snapshot = new RiskCaseSnapshot(
                new RiskCaseId(resultSet.getLong("id")),
                new CaseNumber(resultSet.getString("case_number")),
                new TradingAccountSubjectRef(
                        new TradingAccountRef(resultSet.getString("subject_ref"))),
                CaseIntakeSource.valueOf(resultSet.getString("intake_source")),
                resultSet.getString("intake_summary"),
                RiskCaseStatus.valueOf(resultSet.getString("status")),
                RiskCasePriority.valueOf(resultSet.getString("priority")),
                assignment,
                decisionRef == null ? null : new DecisionRef(decisionRef),
                new ResolutionCycleNumber(resultSet.getInt("current_cycle_no")),
                new ActorRef(resultSet.getString("created_by_ref")),
                resultSet.getTimestamp("created_at").toInstant(),
                new ActorRef(resultSet.getString("updated_by_ref")),
                resultSet.getTimestamp("updated_at").toInstant(),
                resultSet.getLong("version"));
        return RiskCase.rehydrate(snapshot);
    }

    private void bindRootInsert(
            PreparedStatement statement,
            RiskCaseSnapshot snapshot,
            byte[] keyHash,
            byte[] requestHash) throws java.sql.SQLException {
        statement.setString(1, snapshot.caseNumber().value());
        statement.setString(2, snapshot.subjectRef().value());
        statement.setString(3, snapshot.intakeSource().name());
        statement.setString(4, snapshot.intakeSummary());
        statement.setString(5, snapshot.status().name());
        statement.setString(6, snapshot.priority().name());
        statement.setString(7, assignee(snapshot));
        statement.setString(8, assignedBy(snapshot));
        statement.setTimestamp(9, assignedAt(snapshot));
        statement.setString(10, value(snapshot.currentDecisionRef()));
        statement.setInt(11, snapshot.currentCycle().value());
        statement.setBytes(12, keyHash);
        statement.setBytes(13, requestHash);
        statement.setString(14, snapshot.createdBy().value());
        statement.setTimestamp(15, Timestamp.from(snapshot.createdAt()));
        statement.setString(16, snapshot.updatedBy().value());
        statement.setTimestamp(17, Timestamp.from(snapshot.updatedAt()));
        statement.setLong(18, snapshot.version());
    }

    private String prefixedRootColumns(String alias) {
        return ROOT_COLUMNS.lines()
                .flatMap(line -> List.of(line.split(",")).stream())
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> alias + "." + value)
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
    }

    private String assignee(RiskCaseSnapshot snapshot) {
        return snapshot.assignment() == null ? null : snapshot.assignment().assignee().value();
    }

    private String assignedBy(RiskCaseSnapshot snapshot) {
        return snapshot.assignment() == null ? null : snapshot.assignment().assignedBy().value();
    }

    private Timestamp assignedAt(RiskCaseSnapshot snapshot) {
        return snapshot.assignment() == null
                ? null
                : Timestamp.from(snapshot.assignment().assignedAt());
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String value(ActorRef value) {
        return value == null ? null : value.value();
    }

    private String value(DecisionRef value) {
        return value == null ? null : value.value();
    }

    private String value(EvidenceRef value) {
        return value == null ? null : value.value();
    }

    private String value(ActionOutcomeRef value) {
        return value == null ? null : value.value();
    }

    private EvidenceRef evidenceRef(String value) {
        return value == null ? null : new EvidenceRef(value);
    }

    private ActionOutcomeRef actionOutcomeRef(String value) {
        return value == null ? null : new ActionOutcomeRef(value);
    }

    private Long nullableLong(java.sql.ResultSet resultSet, String column)
            throws java.sql.SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private void setLong(PreparedStatement statement, int index, Long value)
            throws java.sql.SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private long requireKey(KeyHolder keys) {
        Number key = keys.getKey();
        if (key == null) {
            throw new IllegalStateException("insert returned no generated identity");
        }
        return key.longValue();
    }

    private <T> Optional<T> single(List<T> rows) {
        if (rows.size() > 1) {
            throw new IllegalStateException("unique query returned multiple rows");
        }
        return rows.stream().findFirst();
    }
}
