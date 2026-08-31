package com.brokeros.risk.decision.infrastructure.persistence;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import com.brokeros.risk.decision.application.CompletedDecisionOperation;
import com.brokeros.risk.decision.application.DecisionAuthorityUnavailableException;
import com.brokeros.risk.decision.domain.ConclusionText;
import com.brokeros.risk.decision.domain.DecisionOperationId;
import com.brokeros.risk.decision.domain.DecisionOperationOutcome;
import com.brokeros.risk.decision.domain.DecisionOperationType;
import com.brokeros.risk.decision.domain.DecisionRecord;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.decision.domain.DecisionSemanticFingerprint;
import com.brokeros.risk.decision.domain.DecisionSource;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.springframework.jdbc.core.ResultSetExtractor;

final class JdbcDecisionRowMappers {

    private JdbcDecisionRowMappers() {
    }

    static ResultSetExtractor<Optional<DecisionRecord>> decisionRecord() {
        return resultSet -> {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            DecisionRow row = decisionRow(resultSet);
            Set<EvidenceRef> evidenceRefs = new LinkedHashSet<>();
            addEvidenceRef(resultSet, evidenceRefs);
            while (resultSet.next()) {
                DecisionRow next = decisionRow(resultSet);
                if (row.id() != next.id() || !row.equals(next)) {
                    throw new DecisionAuthorityUnavailableException();
                }
                addEvidenceRef(resultSet, evidenceRefs);
            }
            return Optional.of(row.toRecord(evidenceRefs));
        };
    }

    static ResultSetExtractor<Optional<CompletedDecisionOperation>> completedOperation() {
        return resultSet -> {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            DecisionOperationId operationId = new DecisionOperationId(
                    resultSet.getString("operation_id"));
            DecisionOperationType operationType = DecisionOperationType.valueOf(
                    resultSet.getString("operation_type"));
            DecisionSemanticFingerprint fingerprint = new DecisionSemanticFingerprint(
                    resultSet.getBytes("semantic_fingerprint"));
            DecisionOperationOutcome outcome = DecisionOperationOutcome.valueOf(
                    resultSet.getString("outcome"));
            java.time.Instant occurredAt = resultSet.getTimestamp("occurred_at").toInstant();
            DecisionRow row = decisionRow(resultSet);
            Set<EvidenceRef> evidenceRefs = new LinkedHashSet<>();
            addEvidenceRef(resultSet, evidenceRefs);
            while (resultSet.next()) {
                if (!operationId.value().equals(resultSet.getString("operation_id"))
                        || operationType != DecisionOperationType.valueOf(
                                resultSet.getString("operation_type"))
                        || !fingerprint.equals(new DecisionSemanticFingerprint(
                                resultSet.getBytes("semantic_fingerprint")))
                        || outcome != DecisionOperationOutcome.valueOf(
                                resultSet.getString("outcome"))
                        || !occurredAt.equals(resultSet.getTimestamp("occurred_at").toInstant())
                        || !row.equals(decisionRow(resultSet))) {
                    throw new DecisionAuthorityUnavailableException();
                }
                addEvidenceRef(resultSet, evidenceRefs);
            }
            DecisionRecord record = row.toRecord(evidenceRefs);
            return Optional.of(new CompletedDecisionOperation(
                    operationId, operationType, fingerprint,
                    record.decisionRef(), outcome, occurredAt, record));
        };
    }

    private static DecisionRow decisionRow(ResultSet resultSet) throws SQLException {
        return new DecisionRow(
                resultSet.getLong("record_id"),
                new DecisionRef(resultSet.getString("decision_ref")),
                new TradingAccountRef(resultSet.getString("subject_ref")),
                DecisionSource.valueOf(resultSet.getString("source")),
                new ConclusionText(decodeUtf8(resultSet.getBytes("conclusion_text"))),
                new ActorRef(resultSet.getString("recorded_by_actor_ref")),
                resultSet.getTimestamp("recorded_at").toInstant());
    }

    private static void addEvidenceRef(
            ResultSet resultSet,
            Set<EvidenceRef> evidenceRefs) throws SQLException {
        String value = resultSet.getString("evidence_ref");
        if (value == null || !evidenceRefs.add(new EvidenceRef(value))) {
            throw new DecisionAuthorityUnavailableException();
        }
    }

    private static String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException(
                    "stored decision content is not valid UTF-8", exception);
        }
    }

    private record DecisionRow(
            long id,
            DecisionRef decisionRef,
            TradingAccountRef subjectRef,
            DecisionSource source,
            ConclusionText conclusionText,
            ActorRef recordedByActorRef,
            java.time.Instant recordedAt) {

        DecisionRecord toRecord(Set<EvidenceRef> evidenceRefs) {
            return new DecisionRecord(
                    decisionRef, subjectRef, evidenceRefs, conclusionText,
                    source, recordedByActorRef, recordedAt);
        }
    }
}
